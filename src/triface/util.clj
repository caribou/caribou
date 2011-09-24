(ns triface.util
  (:use [clojure.string :only (join)]))

(import java.sql.SQLException)

(defn seq-to-map [f q]
  (reduce #(assoc %1 (f %2) %2) {} q))

(defn slugify [s]
  (.toLowerCase (join "_" (re-seq #"[a-zA-Z]+" s))))

(defn render-exception [e]
  (let [cause (.getCause e)]
    (if cause
      (if (isa? cause SQLException)
        (let [next (.getNextException cause)]
          (str next (.printStackTrace next)))
        (str cause (.printStackTrace cause)))
      (str e (.printStackTrace e)))))