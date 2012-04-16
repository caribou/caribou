(ns caribou.tasks.bootstrap
  (:require [clojure.java.jdbc :as sql]
            [caribou.db :as db]
            [caribou.config :as config]
            [caribou.migration :as mm]))

(defn bootstrap [name]
  (db/rebuild-database name)
  (sql/with-connection @config/db
    (mm/run-migrations name)))

(defn -main [name]
  (bootstrap name))