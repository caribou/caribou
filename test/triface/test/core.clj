(ns triface.test.core
  (:use [triface.core])
  (:use [clojure.test])
  (:require [triface.db :as db]))

;; simple content-list test, mocking the underlying db/query call to return an expected result
(deftest content-list-test
  (binding [db/query (fn [query & args] (array-map :id 1 :name "model"))]
    (is (> (count (content-list "model")) 0))))
