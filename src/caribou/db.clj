(ns caribou.db
  (:use [caribou.debug])
  (:use [clojure.string :only (join split)])
  (:require [clojure.string :as string]
            [clojure.java.jdbc :as sql]
            [caribou.app.config :as config]))

(import java.util.regex.Matcher)

(defn zap
  "quickly sanitize a potentially dirty string in preparation for a sql query"
  [s]
  (cond
   (string? s) (.replaceAll (re-matcher #"[\\\"]" (.replaceAll (str s) "'" "''")) "")
   (keyword? s) (zap (name s))
   :else s))

(defn clause
  "substitute values into a string template based on numbered % parameters"
  [pred args]
  (letfn [(rep [s i] (.replaceAll s (str "%" (inc i))
                                  (let [item (nth args i)]
                                    (Matcher/quoteReplacement
                                     (cond
                                      (keyword? item) (name item)
                                      :else
                                      (str item))))))]
    (if (empty? args)
      pred
      (loop [i 0 retr pred]
        (if (= i (-> args count dec))
          (rep retr i)
          (recur (inc i) (rep retr i)))))))

(defn query
  "make an arbitrary query, substituting in extra args as % parameters"
  [q & args]
  (sql/with-query-results res
    [(log :db (clause q args))]
    (doall res)))

(defn recursive-query [table fields base-where recur-where]
  (let [field-names (distinct (map name (concat [:id :parent_id] fields)))
        field-list (join "," field-names)]
    (query (str "with recursive %1_tree(" field-list
                ") as (select " field-list
                " from %1 where %2 union select "
                (join "," (map #(str "%1." %) field-names))
                " from %1,%1_tree where %3)"
                " select * from %1_tree") (name table) base-where recur-where)))

(defn sqlize
  "process a raw value into a sql appropriate string"
  [value]
  (cond
   (number? value) value
   (isa? (type value) Boolean) value
   (keyword? value) (zap (name value))
   (string? value) (str "'" (zap value) "'")
   :else (str "'" (zap (str value)) "'")))

(defn value-map
  "build a string of values fit for an insert or update statement"
  [values]
  (join ", " (map #(str (name %) " = " (sqlize (values %))) (keys values))))

(defn insert
  "insert a row into the given table with the given values"
  [table values]
  ;; (let [keys (join "," (map sqlize (keys mapping)))
  ;;       values (join "," (map sqlize (vals mapping)))
  ;;       q (clause "insert into %1 (%2) values (%3)" [(zap (name table)) keys values])]
  ;;   (sql/with-connection db
  ;;     (sql/do-commands
  ;;       (log :db q)))))
  (log :db (clause "insert into %1 values %2" [(name table) (value-map values)]))
  (sql/insert-record table values))

(defn update
  "update the given row with the given values"
  [table values & where]
  (let [v (value-map values)
        q (clause "update %1 set %2 where " [(zap (name table)) v])
        w (clause (first where) (rest where))
        t (str q w)]
    (sql/do-commands (log :db t))))

(defn delete
  "delete out of the given table according to the supplied where clause"
  [table & where]
  (log :db (clause "delete from %1 values %2" [(name table) (clause (first where) (rest where))]))
  (sql/delete-rows table [(if (not (empty? where)) (clause (first where) (rest where)))]))

(defn fetch
  "pull all items from a table according to the given conditions"
  [table & where]
  (apply query (cons (str "select * from %" (count where) " where " (first where))
                     (concat (rest where) [(name table)]))))

(defn choose
  "pull just the record with the given id from the given table"
  [table id]
  (if id
    (first (query "select * from %1 where id = %2" (zap (name table)) (zap (str id))))
    nil))

;; table operations -------------------------------------------

(defn table?
  "check to see if a table by the given name exists"
  [table]
  (< 0 (count (query "select true from pg_class where relname='%1'" (zap (name table))))))

(defn create-table
  "create a table with the given columns, of the format
  [:column_name :type & :extra]"
  [table & fields]
  (log :db (clause "create table %1 %2" [(name table) fields]))
  (apply sql/create-table (cons table fields)))

(defn rename-table
  "change the name of a table to new-name."
  [table new-name]
  (let [rename (log :db (clause "alter table %1 rename to %2" [(name table) (name new-name)]))]
    (sql/do-commands rename)))

(defn drop-table
  "remove the given table from the database."
  [table]
  (log :db (clause "drop table %1" [(name table)]))
  (sql/drop-table (name table)))

(defn add-column
  "add the given column to the table."
  [table column opts]
  (let [type (join " " (map name opts))]
    (sql/do-commands
     (log :db (clause "alter table %1 add column %2 %3" (map #(zap (name %)) [table column type]))))))

(defn set-default
  "sets the default for a column"
  [table column default]
  (let [value (sqlize default)]
    (sql/do-commands
     (log :db (clause "alter table %1 alter column %2 set default %3" [(zap table) (zap column) value])))))

(defn rename-column
  "rename a column in the given table to new-name."
  [table column new-name]
  (let [rename (log :db (clause "alter table %1 rename column %2 to %3" (map name [table column new-name])))]
    (sql/do-commands rename)))

(defn drop-column
  "remove the given column from the table."
  [table column]
  (sql/do-commands
   (log :db (clause "alter table %1 drop column %2" (map #(zap (name %)) [table column])))))

(defn do-sql
  "execute arbitrary sql.  direct proxy to sql/do-commands."
  [commands]
  (sql/do-commands commands))

(defn change-db-keep-host
  "given the current db config, change the database but keep the hostname"
  [db-config new-db]
  (assoc db-config
    :subname (string/replace (db-config :subname) #"[^/]+$" new-db)))
;; (str "//" (first (split (replace (db-config :subname) "//" "") #"/")) "/" new-db)))

(defn drop-database
  "drop a database of the given name"
  [name]
  (try
    (sql/with-connection (change-db-keep-host @config/db "template1")
      (with-open [s (.createStatement (sql/connection))]
        (.addBatch s (str "drop database " (zap name)))
        (seq (.executeBatch s))))
    (catch Exception e
      (try
        (println (str "database " name " doesn't exist: " (.getNextException (debug e))))
        (catch Exception e (println e))))))

(defn create-database
  "create a database of the given name"
  [name]
  (try
    (sql/with-connection (change-db-keep-host @config/db "template1") 
      (with-open [s (.createStatement (sql/connection))]
        (.addBatch s (str "create database " (zap name)))
        (seq (.executeBatch s))))
    (catch Exception e (println (str "database " name " already exists: " (.getNextException e))))))

(defn rebuild-database
  "drop and recreate the given database"
  [name]
  (drop-database name)
  (create-database name))

(defn call
  [f]
  (sql/with-connection @config/db (f)))

(defn wrap-db
  [handler db & [opts]]
  (fn [request]
    (sql/with-connection db (handler request))))

(defn tally
  "return how many total records are in this table"
  [table]
  ((first (query "select count(id) from %1" (name table))) :count))

