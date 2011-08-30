(ns triface.test.model
  (:require [triface.db :as db])
  (:use [triface.debug])
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
  (try
    (let [yellow-row (create-model {:name "yellow"
                                    :description "yellowness yellow yellow"
                                    :position 3
                                    :fields [{:name "gogon" :type "string"}
                                             {:name "wibib" :type "boolean"}]})

          zap-row (create-model {:name "zap"
                                 :description "zap zappity zapzap"
                                 :position 3
                                 :fields [{:name "ibibib" :type "string"}
                                          {:name "yellows" :type "collection" :target_id (yellow-row :id)}]})

          yellow (model-for :yellow)
          zap (model-for :zap)

          zzzap (create-content :zap {:ibibib "kkkkkkk"})
          yyy (create-content :yellow {:gogon "obobo" :wibib true :zap_id (zzzap :id)})
          yyyz (create-content :yellow {:gogon "igigi" :wibib false :zap_id (zzzap :id)})
          yy (create-content :yellow {:gogon "lalal" :wibib true :zap_id (zzzap :id)})]
      (update-content :yellow (yyy :id) {:gogon "binbin"})
      (is (= ((db/choose :yellow (yyy :id)) :gogon) "binbin"))
      (is (= "kkkkkkk" ((from zap zzzap {:include {}}) :ibibib)))
      (is (= 3 (count ((from zap zzzap {:include {:yellows {}}}) :yellows)))))
    (catch Exception e (throw e))
    (finally 
     
     (if (db/table? :yellow) (delete-model :yellow))
     (if (db/table? :zap) (delete-model :zap))

     )))



