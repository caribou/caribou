(ns caribou.app.controller
  (:require [clojure.java.io :as io]))

;; (defn reset-action []
;;   (def action nil))

;; (reset-action)

(defn- clj-file?
  [filename]
  (re-seq #".clj$" filename))

(defn- base-name
  [filename]
  (last (re-find #"^(.*).clj$" filename)))

(defn- merge-controller
  [controllers controller-file]
  (let [filename (.getName controller-file)]
    (if (clj-file? filename)
      (let [controller-map (assoc controllers (base-name filename) {})]
        )
      controllers)))
      
(defn- init
  []
  (let [files (file-seq (io/file "app/controller"))
        controller-map (reduce merge-controller {} files)]))

(def controllers (ref {}))

(defn controller
  [name & actions]
  (let [original (or (@controllers (keyword name)) {})
        action-map (apply hash-map actions)]
    (dosync
     (alter controllers assoc (keyword name) (merge original action-map)))))
          
  



