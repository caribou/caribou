(ns caribou.app.controller
  (:require [clojure.java.io :as io]
            [caribou.util :as util]))

(def controllers (ref {}))

(defn load-controllers
  [path]
  (util/load-path path
   (fn [file filename]
     (load-file (.toString file)))))

(defn controller
  [name & actions]
  (let [original (or (@controllers (keyword name)) {})
        action-map (apply hash-map actions)]
    (dosync
     (alter controllers assoc (keyword name) (merge original action-map)))))


