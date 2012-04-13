(use 'caribou.model)

(defn migrate []
  (create :model (quote {:name "Fuchsia", :description "fuchfuchsia siasiasia fuchsia", :position 3, :fields [{:name "Zozoz", :type "string"} {:name "Chartreusii", :type "link", :dependent true, :target_id 121}]}) {:op :migration}))
(migrate)
