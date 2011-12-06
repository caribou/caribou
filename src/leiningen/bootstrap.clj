(ns leiningen.bootstrap
  (require [clojure.java.jdbc :as sql]
           [caribou.db :as db]
           [caribou.app.config :as config]
           [caribou.migration :as mm]))

(defn bootstrap [name]
  (db/rebuild-database name)
  (sql/with-connection @config/db
    (mm/run-migrations name)))