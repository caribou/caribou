(ns leiningen.bootstrap
  (require [triface.db :as db])
  (require [triface.migration :as mm]))

(defn bootstrap []
  (db/recreate-database)
  (mm/run-migrations))