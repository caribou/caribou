(ns triface.test.model
  (:require [triface.db :as db])
  (:use [triface.model])
  (:use [clojure.test]))

(deftest create-model-test
  (is (>= 8 (count ((create-model {:name "yellow"
                                   :description "yellowness yellow yellow"
                                   :position 3
                                   :fields [{:name "gogon" :type "string"}
                                            {:name "wibib" :type "boolean"}]}) :fields))))
  (is ((models :yellow) :name "yellow"))
  (is (db/table? "yellow"))

  (delete-model "yellow")

  (is (not (db/table? "yellow")))
  (is (not (models :yellow))))