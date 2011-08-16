(ns leiningen.bootstrap
  (require [triface.db :as db])
  (require [triface.migration :as mm]))

(defn bootstrap []
  (db/drop-schema)
  (mm/run-migrations))