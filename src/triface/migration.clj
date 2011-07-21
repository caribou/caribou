(ns triface.migration
  (use [triface.debug])
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
  (do
    (if (debug (not (db/table? "migration")))
      (map run-migration premigration-list))
    (debug (not (db/table? "migration")))
    (let [names (migration-names)]
      (map #(if (not (some #{%} names)) (run-migration %)) @migration-list))))


