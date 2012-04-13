(use 'caribou.model)

(defn migrate []
  (update :model (quote {:fields [{:id 1405, :name "Okokok"}]}) {:op :migration}))
(migrate)
