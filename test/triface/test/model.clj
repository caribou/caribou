(ns triface.test.model
  (:require [triface.db :as db])
  (:use [triface.model])
  (:use [clojure.test]))

(deftest invoke-model-test
  (let [model (db/query "select * from model where id = 1")
        invoked (invoke-model (first model))]
    (is (= "name" (:name (:row (:name (invoked :fields))))))))

(deftest model-lifecycle-test
  (invoke-models)
  (let [model (create-model {:name "yellow"
                             :description "yellowness yellow yellow"
                             :position 3
                             :fields [{:name "gogon" :type "string"}
                                      {:name "wibib" :type "boolean"}]})
        yellow (db/insert :yellow {:gogon "obobo" :wibib true})]

    (is (<= 8 (count (model :fields))))
    (is (= (model :name) "yellow"))
    (is ((models :yellow) :name "yellow"))
    (is (db/table? :yellow))
    (is (yellow :wibib))
    (is (= 1 (count (db/query "select * from yellow"))))
    
    (delete-model :yellow)

    (is (not (db/table? :yellow)))
    (is (not (models :yellow)))))

(deftest model-interaction-test
  (invoke-models)
  (let [yellow (create-model {:name "yellow"
                             :description "yellowness yellow yellow"
                             :position 3
                             :fields [{:name "gogon" :type "string"}
                                      {:name "wibib" :type "boolean"}]})

        zap (create-model {:name "zap"
                           :description "zap zappity zapzap"
                           :position 3
                           :fields [{:name "ibibib" :type "string"}
                                    {:name "yellows" :type "collection" :target_id (yellow :id)}]})

        yyy (db/insert :yellow {:gogon "obobo" :wibib true})
        yyyz (db/insert :yellow {:gogon "igigi" :wibib false})
        yy (db/insert :yellow {:gogon "lalal" :wibib true})
        zzzap (db/insert :zap {:ibibib "kkkkkkk"})]
    (db/update :yellow {:gogon "binbin"} "id = %1" (yyy :id))
    (is (= ((db/choose :yellow (yyy :id)) :gogon) "binbin"))
    (delete-model :yellow)
    (delete-model :zap)))



