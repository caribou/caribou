(ns caribou.test.api
  (:use [caribou.api]
        [clojure.test]
        [caribou.debug])
  (:require [clojure.java.jdbc :as sql]
            [caribou.db :as db]
            [caribou.model :as model]
            [caribou.app.config :as config]
            [clojure.data.json :as json]))

(deftest content-list-test
  (sql/with-connection @config/db
    (is (> (count (content-list "model" {})) 0))))

(deftest content-item-test
  (sql/with-connection @config/db
    (is (> (count (content-item "model" 1)) 0))))

(deftest content-field-test
  (sql/with-connection @config/db
    (is (= "model" (content-field "model" 1 :slug)))))

;; TODO: test timestamp fields
;; (deftest render-test
;;   (sql/with-connection @config/db
;;     (let [model (render :model (db/fetch) {})]
;;       (is (not (model nil)))
;;       (is (= (model :name) "Foo"))
;;       (is (= (model :description) "bar"))
;;       (is (= (model :position) 1))
;;       (is (= (model :nested) false))
;;       (is (= (model :abstract) false))
;;       (is (= (model :ancestor_id) 0)))))

(deftest render-field-test
  (sql/with-connection @config/db
    (is (= "yayay" (render-field "model" {:description "yayay"} "description" {})))))

;; actions ------------------------------------------------
;; happy-path action smoke-testing

;; GET home
(deftest home-action-test
  (sql/with-connection @config/db
    (let [response (json/read-json (home {}))]
      (is (not (response nil))))))

;; GET list-all
(deftest list-all-action-test
  (sql/with-connection @config/db
    (let [response (json/read-json (list-all {:slug "model"}))]
      (is (> (count response) 0)))))

;; TODO
;; POST create-content
(deftest create-content-action-test)

;; GET model-spec
(deftest model-spec-action-test
  (sql/with-connection @config/db
    (let [response (json/read-json (model-spec {:slug "model"}))]
      (is (> (count response) 0)))))

;; GET item-detail
(deftest item-detail-action-test
  (sql/with-connection @config/db
    (let [response (json/read-json (item-detail {:slug "model" :id 1}))]
      (is (> (count response) 0)))))

;; TODO
;; PUT update-content
(deftest update-content-action-test)

;; TODO
;; PUT delete-content
(deftest delete-content-action-test)

;; GET field-detail
;; (deftest field-detail-action-test
;;   (sql/with-connection @config/db
;;     (let [response (json/read-json (field-detail {:slug "model" :id 1 :field "name"}))]
;;       (is (= "model" response)))))