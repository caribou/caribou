(in-ns 'triface.migration)
(use 'triface.model)

(defn build-links []
  (invoke-models)

  (add-fields (models :site) [{:name "domains"
                               :type "collection"
                               :target_id ((models :domain) :id)}
                              {:name "pages"
                               :type "collection"
                               :target_id ((models :page) :id)}]))

(def migrate (fn []
               (build-links)))

