(ns triface.test.model
  (:require [triface.db :as db])
  (:use [triface.model])
  (:use [clojure.test]))

(create-model {:name "yellow" :description "yellowness yellow yellow" :position 3 :fields [{:name "gogon" :type "string"} {:name "wibib" :type "boolean"}]})

(deftest create-model-test
  (let [model (create-model {:name "yellow"
                             :description "yellowness yellow yellow"
                             :position 3
                             :fields [{:name "gogon" :type "string"}
                                      {:name "wibib" :type "boolean"}]})]
    (is (>= 8 (count (model :fields))))
    (is (= (model :name) "yellow"))
    (is ((models :yellow) :name "yellow"))
    (is (db/table? "yellow"))

    (delete-model "yellow")

    (is (not (db/table? "yellow")))
    (is (not (models :yellow)))))

