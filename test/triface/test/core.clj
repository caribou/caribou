(ns triface.test.core
  (:use [triface.core])
  (:use [clojure.test])
  (:require [triface.db :as db]))

(deftest content-list-test
  (binding [db/query (fn [query & args] (array-map :id 1 :name "model"))]
    (is (> (count (content-list "model")) 0))))

(deftest content-item-test
  (binding [db/query (fn [query & args] (array-map :id 1))]
    (is (> (count (content-item "model" 1)) 0))))

(deftest content-field-test
  (binding [db/query (fn [query & args] (vector (array-map :id 1 :name "model")))]
    (is (="model" (content-field "model" 1 :name)))))

;; TODO: test timestamp fields
(deftest render-test
  (let [model (render "model" (array-map :id 1 :name "foo" :description "bar" :position 1 :nested false :locked true :abstract false :ancestor_id 0))]
    (is (not (model nil)))
    (is (= (model :name) "foo"))
    (is (= (model :description) "bar"))
    (is (= (model :position) 1))
    (is (= (model :nested) false))
    (is (= (model :abstract) false))
    (is (= (model :ancestor_id) 0))))

(deftest render-field-test
  (is (="foo" (render-field "model" (array-map :id 1 :name "foo") "name"))))