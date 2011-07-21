(ns leiningen.migrate
  (require [triface.migration :as mm]))

(defn migrate []
  (mm/run-migrations))