(ns triface.db
  (:use [triface.debug])
  (:use [clojure.contrib.str-utils])
  (:require [clojure.java.jdbc :as sql]))

(def db
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname "//localhost/triface"
   :user "postgres"})

(defn sanitize [s]
  (.replaceAll (re-matcher #"[\\\";#%]" (.replaceAll (str s) "'" "''")) ""))

(defn clause [pred args]
  (letfn [(rep [s i] (.replaceAll s (str "%" (inc i))
                                  (let [item (sanitize (nth args i))]
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

(defn query [q & args]
  (sql/with-connection db
    (sql/with-query-results res
      [(debug (clause q args))]
      (doall res))))

(defn insert [table values]
  (sql/with-connection db
    (sql/insert-record table values)))

(defn delete [table & where]
  (sql/with-connection db
    (sql/delete-rows table [(if (not (empty? where)) (clause (first where) (rest where)))])))

(defn choose [table id]
  (first (query "select * from %1 where id = %2" (name table) id)))

(defn table? [table]
  (< 0 (count (query "select true from pg_class where relname='%1'" (name table)))))

(defn create-table [table & fields]
  (sql/with-connection db
    (apply sql/create-table (cons table fields))))

(defn drop-table [table]
  (sql/with-connection db
    (sql/drop-table (name table))))

(defn drop-schema []
  (sql/with-connection db
    (sql/do-commands "drop schema public cascade")))

(defn add-column [table column opts]
  (let [type (str-join " " (map name opts))]
    (sql/with-connection db
      (sql/do-commands
       (debug (clause "alter table %1 add column %2 %3" (map name [table column type])))))))
