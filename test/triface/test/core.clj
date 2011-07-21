(ns triface.test.core
  (:use [triface.core])
  (:use [clojure.test]))

(deftest core-test
  (is true (> (count (content-list "model")) 0)))


