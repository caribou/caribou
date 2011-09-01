(ns triface.util
  (:require [clojure.contrib.json :as json])
  (:use clojure.contrib.str-utils))

(defn seq-to-map [f q]
  (reduce #(assoc %1 (f %2) %2) {} q))

(defn slugify [s]
  (.toLowerCase (str-join "_" (re-seq #"[a-zA-Z]+" s))))