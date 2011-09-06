(in-ns 'triface.migration)

(defn lock [fields]
  (map #(assoc % :locked true) fields))

(def page {:name "page"
           :description "center of all elements for a single request"
           :position 3
           :locked true
           :nested true
           :fields (lock [{:name "name" :type "string"}
                          {:name "slug" :type "slug" :link_slug "name"}
                          {:name "action" :type "string"}
                          {:name "parent_id" :type "integer"}])})

(def view {:name "view"
           :description "a composition of content facets"
           :position 4
           :locked true
           :fields (lock [{:name "name" :type "string"}
                          {:name "description" :type "text"}])})

(def locale {:name "locale"
             :description "a collection of content for a particular geographical audience"
             :position 6
             :locked true
             :fields (lock [{:name "language" :type "string"}
                            {:name "region" :type "string"}
                            {:name "code" :type "string"}
                            {:name "description" :type "text"}])})

(def asset {:name "asset"
            :description "a reference to some system resource"
            :position 7
            :locked true
            :fields (lock [{:name "name" :type "string"}
                           {:name "url" :type "string"}
                           {:name "content_type" :type "string"}
                           {:name "parent_id" :type "integer"}
                           {:name "description" :type "text"}])})

(def site {:name "site"
           :description "maps to a particular set of pages"
           :position 8
           :locked true
           :fields (lock [{:name "name" :type "string"}
                          {:name "slug" :type "slug" :link_slug "name"}
                          {:name "description" :type "text"}])})

(def domain {:name "domain"
             :description "each site may have several domain names that direct to its page set"
             :position 9
             :locked true
             :fields (lock [{:name "name" :type "string"}
                            {:name "description" :type "text"}])})

(def incubating
  [page view locale asset site domain])

(defn spawn-models []
  (doall (map model/create-model incubating)))

(def migrate
  (fn []
    (spawn-models)))

