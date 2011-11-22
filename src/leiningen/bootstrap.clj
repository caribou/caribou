(ns leiningen.bootstrap
  (require [caribou.db :as db])
  (require [caribou.migration :as mm]))

(defn bootstrap [name]
  (db/rebuild-database name)
  (mm/run-migrations name))