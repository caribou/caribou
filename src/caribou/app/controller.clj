(ns caribou.app.controller
  (:require [clojure.java.io :as io]
            [caribou.util :as util]))

;; (defn- clj-file?
;;   [filename]
;;   (re-seq #".clj$" filename))

;; (defn- base-name
;;   [filename]
;;   (last (re-find #"^(.*).clj$" filename)))

;; (defn- merge-controller
;;   [controllers controller-file]
;;   (let [filename (.getName controller-file)]
;;     (if (clj-file? filename)
;;       (let [controller-map (assoc controllers (base-name filename) {})]
;;         )
;;       controllers)))
      
;; (defn- init
;;   []
;;   (let [files (file-seq (io/file "app/controller"))
;;         controller-map (reduce merge-controller {} files)]))

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


