(use 'caribou.model)

(defn migrate []
  (create :model (quote {:name "White", :fields [{:name "Grey", :type "string"}], :nested true}) {:op :migration}))
(migrate)
