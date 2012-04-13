(use 'caribou.model)

(defn migrate []
  (create :model (quote {:name "Zap", :description "zap zappity zapzap", :position 3, :fields [{:name "Ibibib", :type "string"} {:name "Yobob", :link_slug "ibibib", :type "slug"} {:name "Yellows", :type "collection", :dependent true, :target_id 124}]}) {:op :migration}))
(migrate)
