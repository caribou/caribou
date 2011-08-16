(in-ns 'triface.migration)
(use 'triface.model)
(use 'triface.debug)

(defn build-links []
  (invoke-models)
  (debug ((models :field) :id))

  (add-fields (models :site) [{:name "domains"
                               :type "collection"
                               :target_id ((models :domain) :id)}
                              {:name "pages"
                               :type "collection"
                               :target_id ((models :page) :id)}]))

(def migrate (fn []
               (build-links)))

