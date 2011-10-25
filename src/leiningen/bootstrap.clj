(ns leiningen.bootstrap
  (require [triface.db :as db])
  (require [triface.migration :as mm]))

(defn bootstrap [name]
  (db/rebuild-database name)
  (mm/run-migrations name))