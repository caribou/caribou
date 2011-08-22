(ns triface.db
  (:use [triface.debug])
  (:use [clojure.contrib.str-utils])
  (:require [clojure.java.jdbc :as sql]))

(def db
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname "//localhost/triface"
   :user "postgres"})

(defn zap [s]
  (.replaceAll (re-matcher #"[\\\";#%]" (.replaceAll (str s) "'" "''")) ""))

(defn clause [pred args]
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

(defn query [q & args]
  (sql/with-connection db
    (sql/with-query-results res
      [(log :db (clause q args))]
      (doall res))))

(defn value-map [values]
  (str-join ", " (map #(str (name %) " = '" (zap (values %)) "'") (keys values))))

(defn insert [table values]
  (log :db (clause "insert into %1 values %2" [(name table) (value-map values)]))
  (sql/with-connection db
    (sql/insert-record table values)))

(defn update [table values & where]
  (let [v (value-map values)
        q (clause "update %1 set %2 where " [(zap (name table)) v])
        w (clause (first where) (rest where))
        t (str q w)]
    (sql/with-connection db
      (sql/do-commands (log :db t)))))

(defn delete [table & where]
  (log :db (clause "delete from %1 values %2" [(name table) (clause (first where) (rest where))]))
  (sql/with-connection db
    (sql/delete-rows table [(if (not (empty? where)) (clause (first where) (rest where)))])))

(defn fetch [table & where]
  (apply query (cons (str "select * from %" (count where) " where " (first where))
                     (concat (rest where) [(name table)]))))

(defn choose [table id]
  (first (query "select * from %1 where id = %2" (zap (name table)) (zap (str id)))))

(defn table? [table]
  (< 0 (count (query "select true from pg_class where relname='%1'" (zap (name table))))))

(defn create-table [table & fields]
  (log :db (clause "create table %1 %2" [(name table) fields]))
  (sql/with-connection db
    (apply sql/create-table (cons table fields))))

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
