(in-ns 'triface.migration)

(def page {:name "page"
           :description "center of all elements for a single request"
           :position 3
           :locked true
           :nested true
           :fields [{:name "name" :type "string"}
                    {:name "slug" :type "string"}
                    {:name "action" :type "string"}
                    {:name "parent_id" :type "integer"}
                    {:name "model_id" :type "integer"}
                    {:name "site_id" :type "integer"}
                    {:name "name" :type "string"}]})

(def view {:name "view"
           :description "a composition of content facets"
           :position 4
           :locked true
           :fields [{:name "name" :type "string"}
                    {:name "description" :type "text"}]})

(def facet {:name "facet"
           :description "a reference to a particular field"
           :position 5
           :locked true
           :fields [{:name "name" :type "string"}
                    {:name "slug" :type "string"}
                    {:name "field_id" :type "integer"}
                    {:name "view_id" :type "integer"}]})

(def locale {:name "locale"
           :description "a collection of content for a particular geographical audience"
           :position 6
           :locked true
           :fields [{:name "language" :type "string"}
                    {:name "region" :type "string"}
                    {:name "code" :type "string"}
                    {:name "description" :type "text"}]})

(def asset {:name "asset"
           :description "a reference to some system resource"
           :position 7
           :locked true
           :fields [{:name "name" :type "string"}
                    {:name "url" :type "string"}
                    {:name "content_type" :type "string"}
                    {:name "parent_id" :type "integer"}
                    {:name "description" :type "text"}]})

(def site {:name "site"
           :description "maps to a particular set of pages"
           :position 4
           :locked true
           :fields [{:name "name" :type "string"}
                    {:name "slug" :type "string"}
                    {:name "description" :type "text"}]})

(def domain {:name "domain"
             :description "each site may have several domain names that direct to its page set"
             :position 4
             :locked true
             :fields [{:name "name" :type "string"}
                      {:name "site_id" :type "integer"}
                      {:name "description" :type "text"}]})

(def incubating
     [page view facet locale asset site domain])

(defn spawn-models []
  (doall (map model/create-model incubating)))

(def migrate (fn []
  (spawn-models)))

