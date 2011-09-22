(ns triface.db
  (:use [triface.debug])
  (:use [clojure.contrib.str-utils])
  (:require [clojure.contrib.generic.functor :as fun])
  (:require [clojure.java.jdbc :as sql]))

(def db
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname "//localhost/triface"
   :user "postgres"})

(defn zap
  "quickly sanitize a potentially dirty string in preparation for a sql query"
  [s]
  (cond
   (string? s) (.replaceAll (re-matcher #"[\\\";#%]" (.replaceAll (str s) "'" "''")) "")
   (keyword? s) (zap (name s))
   :else s))

(defn clause
  "substitute values into a string template based on numbered % parameters"
  [pred args]
  (letfn [(rep [s i] (.replaceAll s (str "%" (inc i))
                                  (let [item (nth args i)]
                                    (cond
                                     (keyword? item) (name item)
                                     :else
                                     (str item)))))]
    (if (empty? args)
      pred
      (loop [i 0 retr pred]
        (if (= i (-> args count dec))
          (rep retr i)
          (recur (inc i) (rep retr i)))))))

(defn query
  "make an arbitrary query, substituting in extra args as % parameters"
  [q & args]
  (sql/with-connection db
    (sql/with-query-results res
      [(log :db (clause q args))]
      (doall res))))

(defn recursive-query [table fields base-where recur-where]
  (let [field-names (distinct (map name (concat [:id :parent_id] fields)))
        field-list (str-join "," field-names)]
    (query (str "with recursive %1_tree(" field-list
                ") as (select " field-list
                " from %1 where %2 union select "
                (str-join "," (map #(str "%1." %) field-names))
                " from %1,%1_tree where %3)"
                " select * from %1_tree") (name table) base-where recur-where)))

(defn sqlize
  "process raw values into sql appropriate strings"
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
  (str-join ", " (map #(str (name %) " = " (sqlize (values %))) (keys values))))

(defn insert
  "insert a row into the given table with the given values"
  [table values]
  ;; (let [keys (str-join "," (map sqlize (keys mapping)))
  ;;       values (str-join "," (map sqlize (vals mapping)))
  ;;       q (clause "insert into %1 (%2) values (%3)" [(zap (name table)) keys values])]
  ;;   (sql/with-connection db
  ;;     (sql/do-commands
  ;;       (log :db q)))))

  (log :db (clause "insert into %1 values %2" [(name table) (value-map values)]))
  (sql/with-connection db
    (sql/insert-record table values)))

(defn update
  "update the given row with the given values"
  [table values & where]
  (let [v (value-map values)
        q (clause "update %1 set %2 where " [(zap (name table)) v])
        w (clause (first where) (rest where))
        t (str q w)]
    (sql/with-connection db
      (sql/do-commands (log :db t)))))

(defn delete
  "delete out of the given table according to the supplied where clause"
  [table & where]
  (log :db (clause "delete from %1 values %2" [(name table) (clause (first where) (rest where))]))
  (sql/with-connection db
    (sql/delete-rows table [(if (not (empty? where)) (clause (first where) (rest where)))])))

(defn fetch
  "pull all items from a table according to the given conditions"
  [table & where]
  (apply query (cons (str "select * from %" (count where) " where " (first where))
                     (concat (rest where) [(name table)]))))

(defn choose
  "pull just the record with the given id from the given table"
  [table id]
  (first (query "select * from %1 where id = %2" (zap (name table)) (zap (str id)))))

(defn table?
  "check to see if a table by the given name exists"
  [table]
  (< 0 (count (query "select true from pg_class where relname='%1'" (zap (name table))))))

(defn create-table [table & fields]
  (log :db (clause "create table %1 %2" [(name table) fields]))
  (sql/with-connection db
    (apply sql/create-table (cons table fields))))

(defn rename-table [table new-name]
  (let [rename (log :db (clause "alter table %1 rename to %2" [(name table) (name new-name)]))]
    (sql/with-connection db
      (sql/do-commands rename))))

(defn drop-table [table]
  (log :db (clause "drop table %1" [(name table)]))
  (sql/with-connection db
    (sql/drop-table (name table))))

(defn rebuild-table []
  (sql/with-connection (assoc db :subname "//localhost/template1")
    (sql/do-commands "drop database triface" "create database triface")))

(defn add-column [table column opts]
  (let [type (str-join " " (map name opts))]
    (sql/with-connection db
      (sql/do-commands
       (log :db (clause "alter table %1 add column %2 %3" (map #(zap (name %)) [table column type])))))))

(defn rename-column [table column new-name]
  (let [rename (log :db (clause "alter table %1 rename column %2 to %3" (map name [table column new-name])))]
    (sql/with-connection db
      (sql/do-commands rename))))

(defn drop-column [table column]
  (sql/with-connection db
    (sql/do-commands
     (log :db (clause "alter table %1 drop column %2" (map #(zap (name %)) [table column]))))))

(defn do-sql [commands]
  (sql/with-connection db
    (sql/do-commands commands)))

(defn count
  "this aliases clojure.core$count so I keep it here at the bottom"
  [table]
  ((first (query "select count(id) from %1" (name table))) :count))

