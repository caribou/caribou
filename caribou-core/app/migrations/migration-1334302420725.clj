(use 'caribou.model)

(defn migrate []
  (create :model (quote {:position 3, :name "Yellow", :fields [{:name "Gogon", :type "string"} {:name "Wibib", :type "boolean"}], :description "yellowness yellow yellow"}) {:op :migration}))
(migrate)
