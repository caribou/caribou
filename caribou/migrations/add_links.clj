(use 'caribou.model)

(defn build-links []
  (invoke-models)
  (update :model ((models :site) :id)
          {:fields [{:name "Domains"
                     :type "collection"
                     :target_id ((models :domain) :id)}
                    {:name "Pages"
                     :type "collection"
                     :target_id ((models :page) :id)}]}
          {:op :migration}))

(defn migrate
  []
  (build-links))

(migrate)

