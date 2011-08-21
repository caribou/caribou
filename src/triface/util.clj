(ns triface.util
  (:require [clojure.contrib.json :as json]))

(defn seq-to-map [f q]
  (reduce #(assoc %1 (f %2) %2) {} q))

