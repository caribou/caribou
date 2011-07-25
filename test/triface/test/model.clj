(ns triface.test.model
  (:require [triface.db :as db])
  (:use [triface.model])
  (:use [clojure.test]))

(deftest model-lifecycle-test
  (let [model (create-model {:name "yellow"
                             :description "yellowness yellow yellow"
                             :position 3
                             :fields [{:name "gogon" :type "string"}
                                      {:name "wibib" :type "boolean"}]})
        yellow (db/insert :yellow {:gogon "obobo" :wibib true})]

    (is (>= 8 (count (model :fields))))
    (is (= (model :name) "yellow"))
    (is ((models :yellow) :name "yellow"))
    (is (db/table? "yellow"))
    (is (yellow :wibib))
    (is (= 1 (count (db/query "select * from yellow"))))
    
    (delete-model "yellow")

    (is (not (db/table? "yellow")))
    (is (not (models :yellow)))))




