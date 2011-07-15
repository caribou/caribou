(ns triface.migration
  (require [clojure.set :as set]
           [triface.db :as db]))

(def migration-list
     (ref ["create-models"]))

(defn migrations-to-run []
  (let [already-run (map #(% :name) (db/query "select * from migration"))]
    (set/difference @migration-list already-run)))

(defn run-migration [name]
  (intern *ns* (symbol name)))

(defn run-migrations []
  (let [already-run (map #(% :name) (db/query "select * from migration"))]
    (map #(if (not (some #{%} already-run)) (run-migration %)) migration-list)))
    

