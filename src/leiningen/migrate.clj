(ns leiningen.migrate
  (require [triface.migration :as mm]))

(defn migrate [name]
  (mm/run-migrations name))