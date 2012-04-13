(use 'caribou.model)

(defn migrate []
  (update :model (quote {:name "Purple", :fields [{:id 1408, :name "Green"}]}) {:op :migration}))
(migrate)
