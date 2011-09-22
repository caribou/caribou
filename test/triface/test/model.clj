(ns triface.test.model
  (:require [triface.db :as db]
            [triface.util :as util])
  (:use [triface.debug])
  (:use [triface.model])
  (:use [clojure.test]))

(deftest invoke-model-test
  (let [model (db/query "select * from model where id = 1")
        invoked (invoke-model (first model))]
    (is (= "name" (:name (:row (:name (invoked :fields))))))))

(deftest model-lifecycle-test
  (invoke-models)
  (let [model (create :model
         {:name "yellow"
          :description "yellowness yellow yellow"
          :position 3
          :fields [{:name "gogon" :type "string"}
                   {:name "wibib" :type "boolean"}]})
        yellow (create :yellow {:gogon "obobo" :wibib true})]

    (is (<= 8 (count (model :fields))))
    (is (= (model :name) "yellow"))
    (is ((models :yellow) :name "yellow"))
    (is (db/table? :yellow))
    (is (yellow :wibib))
    (is (= 1 (count (db/query "select * from yellow"))))
    
    (destroy :model (model :id))

    (is (not (db/table? :yellow)))
    (is (not (models :yellow)))))


(deftest model-interaction-test
  (invoke-models)
  (try
    (let [yellow-row (create :model
           {:name "yellow"
            :description "yellowness yellow yellow"
            :position 3
            :fields [{:name "gogon" :type "string"}
                     {:name "wibib" :type "boolean"}]})

          zap-row (create :model
            {:name "zap"
             :description "zap zappity zapzap"
             :position 3
             :fields [{:name "ibibib" :type "string"}
                      {:name "yobob" :type "slug" :link_slug "ibibib"}
                      {:name "yellows" :type "collection" :dependent true :target_id (yellow-row :id)}]})

          yellow (models :yellow)
          zap (models :zap)

          zzzap (create :zap {:ibibib "kkkkkkk"})
          yyy (create :yellow {:gogon "obobo" :wibib true :zap_id (zzzap :id)})
          yyyz (create :yellow {:gogon "igigi" :wibib false :zap_id (zzzap :id)})
          yy (create :yellow {:gogon "lalal" :wibib true :zap_id (zzzap :id)})]
      (update :yellow (yyy :id) {:gogon "binbin"})
      (update :zap (zzzap :id)
                      {:ibibib "OOOOOO mmmmm   ZZZZZZZZZZ"
                       :yellows [{:id (yyyz :id) :gogon "IIbbiiIIIbbibib"}
                                 {:gogon "nonononononon"}]})
      
      (let [zap-reload (db/choose :zap (zzzap :id))]
        (is (= ((db/choose :yellow (yyyz :id)) :gogon) "IIbbiiIIIbbibib"))
        (is (= ((db/choose :yellow (yyy :id)) :gogon) "binbin"))
        (is (= (zap-reload :yobob) "oooooo_mmmmm_zzzzzzzzzz"))
        (is (= "OOOOOO mmmmm   ZZZZZZZZZZ" ((from zap zap-reload {:include {}}) :ibibib)))
        (is (= 4 (count ((from zap zap-reload {:include {:yellows {}}}) :yellows))))

        (update :model (zap :id) {:fields [{:id (-> zap :fields :ibibib :row :id)
                                            :name "okokok"}]})

        (update :model (yellow :id) {:name "purple"
                                     :fields [{:id (-> yellow :fields :zap :row :id)
                                               :name "green"}]})

        (let [zappo (db/choose :zap (zzzap :id))
              purple (db/choose :purple (yyy :id))]
          (is (= (zappo :okokok) "OOOOOO mmmmm   ZZZZZZZZZZ"))
          (is (= (purple :green_id) (zappo :id))))

        (destroy :zap (zap-reload :id))
        (let [purples (db/query "select * from purple")]
          (is (empty? purples))))

      (destroy :model (zap :id))

      (is (empty? (-> @models :purple :fields :green_id)))

      (destroy :model (debug (-> @models :purple :id)))

      (is (and (not (db/table? :purple))
               (not (db/table? :yellow))
               (not (db/table? :zap)))))

    (catch Exception e (util/render-exception e))
    (finally      
     (if (db/table? :yellow) (destroy :model (-> @models :yellow :id)))
     (if (db/table? :purple) (destroy :model (-> @models :purple :id)))
     (if (db/table? :zap) (destroy :model (-> @models :zap :id))))))


(deftest nested-model-test
  (invoke-models)
  (try
    (let [white (create :model {:name "white" :nested true :fields [{:name "grey" :type "string"}]})
          aaa (create :white {:grey "obobob"})
          bbb (create :white {:grey "ininin" :parent_id (aaa :id)})
          ccc (create :white {:grey "kkukku" :parent_id (aaa :id)})
          ddd (create :white {:grey "zezeze" :parent_id (bbb :id)})
          eee (create :white {:grey "omomom" :parent_id (ddd :id)})
          fff (create :white {:grey "mnomno" :parent_id (ddd :id)})
          ggg (create :white {:grey "jjijji" :parent_id (ccc :id)})
          fff_path (db/query "with recursive %1_tree(id,grey,parent_id) as (select id,grey,parent_id from %1 where id = %2 union select %1.id,%1.grey,%1.parent_id from %1,%1_tree where %1_tree.parent_id = %1.id) select * from %1_tree" (white :slug) (fff :id))]
      (is (= 4 (count (debug fff_path)))))
    (catch Exception e (util/render-exception e))
    (finally (if (db/table? :white) (destroy :model (-> @models :white :id))))))


          