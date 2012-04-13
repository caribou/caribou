(use 'caribou.model)

(defn migrate []
  (create :model (quote {:position 3, :name "Chartreuse", :fields [{:name "Ondondon", :type "string"} {:name "Kokok", :type "boolean"}], :description "chartreusey reuse chartreuse"}) {:op :migration}))
(migrate)
