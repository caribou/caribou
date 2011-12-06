(ns caribou.migration
  (:require [clojure.set :as set]
            [clojure.java.jdbc :as sql]
            [caribou.db :as db]
            [caribou.app.config :as config]
            [caribou.model :as model]))

(def migrate (fn [] :nothing))

(def premigration-list
     ["create_base_tables"])

(def migration-list
     (ref ["create_models" "create_locked_models" "add_links"]))

(defn push-migration [name]
  (dosync (alter migration-list #(cons name %))))

(defn migration-names []
  (map #(% :name) (db/query "select * from migration")))

(defn migrations-to-run []
  (set/difference @migration-list (migration-names)))

(defn run-migration [name]
  (load (str "caribou/migrations/" name))
  (migrate)
  (db/insert :migration {:name name}))

(defn run-migrations [db-name]
  (try
    (sql/with-connection (merge @config/db {:subname (str "//localhost/" db-name)})
      (if (not (db/table? "migration"))
        (doall (map run-migration premigration-list)))
      (doall (map #(if (not (some #{%} (migration-names))) (run-migration %))
                  @migration-list)))
    (catch Exception e
      (println "Caught an exception attempting to run migrations: " (.getMessage e) (.printStackTrace e)))))



