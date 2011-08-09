(in-ns 'triface.migration)

(def page {:name "page"
           :description "center of all elements for a single request"
           :position 4
           :locked true
           :fields [{:name "name" :type "string"}
                    {:name "slug" :type "string"}
                    {:name "action" :type "string"}
                    {:name "parent_id" :type "integer"}
                    {:name "model_id" :type "integer"}
                    {:name "name" :type "string"}]})

(def view {:name "view"
           :description "a composition of content facets"
           :position 5
           :locked true
           :fields [{:name "name" :type "string"}
                    {:name "description" :type "text"}]})

(def facet {:name "facet"
           :description "a reference to a particular field"
           :position 6
           :locked true
           :fields [{:name "name" :type "string"}
                    {:name "slug" :type "string"}
                    {:name "field_id" :type "integer"}
                    {:name "view_id" :type "integer"}]})

(def locale {:name "locale"
           :description "a collection of content for a particular geographical audience"
           :position 7
           :locked true
           :fields [{:name "language" :type "string"}
                    {:name "region" :type "string"}
                    {:name "code" :type "string"}
                    {:name "description" :type "text"}]})

(def asset {:name "asset"
           :description "a reference to some system resource"
           :position 8
           :locked true
           :fields [{:name "name" :type "string"}
                    {:name "url" :type "string"}
                    {:name "content_type" :type "string"}
                    {:name "parent_id" :type "integer"}
                    {:name "description" :type "text"}]})

(def incubating
     [page view facet locale asset])

(defn spawn-models []
  (doall (map model/create-model incubating)))

(def migrate (fn []
  (spawn-models)))