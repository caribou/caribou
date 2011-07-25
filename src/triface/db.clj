(ns triface.db
  (:use [triface.debug])
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
      [(clause q args)]
      (doall res))))

(defn insert [name values]
  (sql/with-connection db
    (sql/insert-record name values)))

(defn delete [name & where]
  (sql/with-connection db
    (sql/delete-rows name [(if (not (empty? where)) (clause (first where) (rest where)))])))

(defn table? [name]
  (< 0 (count (query "select true from pg_class where relname='%1'" name))))

(defn create-table [name & fields]
  (sql/with-connection db
    (apply sql/create-table (cons name fields))))

(defn drop-table [name]
  (sql/with-connection db
    (sql/drop-table name)))