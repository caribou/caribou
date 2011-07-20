(ns triface.migration
  (require [clojure.set :as set]
           [triface.db :as db]))

(def migrate (fn [] :nothing))

(def premigration-list
     ["create_models"])

(def migration-list
     (ref ["create_models"]))

(defn push-migration [name]
  (dosync (alter migration-list #(cons name %))))

(defn migrations-to-run []
  (let [already-run (map #(% :name) (db/query "select * from migration"))]
    (set/difference @migration-list already-run)))

(defn run-migration [name]
  (load (str "triface/migrations/" name))
  (migrate)
  (db/insert :migration {:name name}))

(defn run-migrations []
  (if (not (db/table? "migration"))
    (map run-migration premigration-list))
  (let [already-run (map #(% :name) (db/query "select * from migration"))]
    (map #(if (not (some #{%} already-run)) (run-migration %)) @migration-list)))






