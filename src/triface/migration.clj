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

(defn migration-names []
  (map #(% :name) (db/query "select * from migration")))

(defn migrations-to-run []
  (set/difference @migration-list (migration-names)))

(defn run-migration [name]
  (load (str "triface/migrations/" name))
  (migrate)
  (db/insert :migration {:name name}))

(defn run-migrations []
  (if (not (db/table? "migration"))
    (doall (map run-migration premigration-list)))
  (map #(if (not (some #{%} (migration-names))) (run-migration %))
       @migration-list))


