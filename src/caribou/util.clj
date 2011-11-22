(ns caribou.util
  (:use [clojure.string :only (split join capitalize)]))

(import java.sql.SQLException)
(import java.io.File)

(defn seq-to-map [f q]
  (reduce #(assoc %1 (f %2) %2) {} q))

(defn slugify [s]
  (.toLowerCase (join "_" (re-seq #"[a-zA-Z]+" s))))

(defn titleize [s]
  (join " " (map capitalize (split s #"[^a-zA-Z]+"))))

(defn render-exception [e]
  (let [cause (.getCause e)]
    (if cause
      (if (isa? cause SQLException)
        (let [next (.getNextException cause)]
          (str next (.printStackTrace next)))
        (str cause (.printStackTrace cause)))
      (str e (.printStackTrace e)))))

(defn get-file-extension [file]
  (let [filename (.getName file)]
  (.toLowerCase (.substring filename (.lastIndexOf filename ".")))
))
