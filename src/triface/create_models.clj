(ns triface.migration
  (require [clojure.set :as set]
           [triface.db :as db]))

(declare run)

(def migration-list
     (ref ["create-models"]))

(defn migrations-to-run []
  (let [already-run (map #(% :name) (db/query "select * from migration"))]
    (set/difference @migration-list already-run)))

(defn run-migration [name]
  (load (str "migrations/" name))
  (run))

(defn run-migrations []
  (let [already-run (map #(% :name) (db/query "select * from migration"))]
    (map #(if (not (some #{%} already-run)) (run-migration %)) migration-list)))






