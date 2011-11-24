(ns leiningen.migrate
  (require [caribou.migration :as mm]))

(defn migrate [name]
  (mm/run-migrations name))