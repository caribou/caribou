(ns triface.db
  (:require [clojure.java.jdbc :as sql]))

(def db
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname "//localhost/triface"
   :user "postgres"})

(defn sanitize [s]
  (.replaceAll (re-matcher #"[\\\";#%]" (.replaceAll s "'" "''")) ""))

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


"IF EXISTS (SELECT relname FROM pg_class WHERE relname='migration') THEN SELECT 1 ELSE SELECT 2 END IF"

(defn query [q & args]
  (sql/with-connection db
    (sql/with-query-results res
      [(clause q args)]
      (into [] res))))

(defn insert [name values]
  (sql/with-connection db
    (sql/insert-record name values)))

(defn table? [name]
  (< 0 (count (query (str "select true from pg_class where relname='" (sanitize name) "'")))))

(defn create-table [name & fields]
  (sql/with-connection db
    (apply sql/create-table (cons name fields))))