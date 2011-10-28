(in-ns 'triface.migration)

(invoke-models)

(defn lock [fields]
  (map #(assoc % :locked true) fields))

(def page {:name "Page"
           :description "center of all elements for a single request"
           :position 3
           :locked true
           :nested true
           :fields (lock [{:name "Name" :type "string"}
                          {:name "Slug" :type "slug" :link_slug "name"}
                          {:name "Path" :type "string"}
                          {:name "Action" :type "string"}
                          {:name "Template" :type "string"}])})

(def view {:name "View"
           :description "a composition of content facets"
           :position 4
           :locked true
           :fields (lock [{:name "Name" :type "string"}
                          {:name "Description" :type "text"}])})

(def locale {:name "Locale"
             :description "a collection of content for a particular geographical audience"
             :position 6
             :locked true
             :fields (lock [{:name "Language" :type "string"}
                            {:name "Region" :type "string"}
                            {:name "Code" :type "string"}
                            {:name "Description" :type "text"}])})

(def asset {:name "Asset"
            :description "a reference to some system resource"
            :position 7
            :locked true
            :fields (lock [{:name "Filename" :type "string"}
                           {:name "Url" :type "string"}
                           {:name "Content Type" :type "string"}
                           {:name "Size" :type "integer"}
                           {:name "Parent Id" :type "integer"}
                           {:name "Description" :type "text"}])})

(def site {:name "Site"
           :description "maps to a particular set of pages"
           :position 8
           :locked true
           :fields (lock [{:name "Name" :type "string"}
                          {:name "Slug" :type "slug" :link_slug "name"}
                          {:name "Asset" :type "asset"}
                          {:name "Description" :type "text"}])})

(def domain {:name "Domain"
             :description "each site may have several domain names that direct to its page set"
             :position 9
             :locked true
             :fields (lock [{:name "Name" :type "string"}
                            {:name "Description" :type "text"}])})

(def location {:name "Location"
               :description "a location somewhere on the planet"
               :position 10
               :locked true
               :fields (lock [{:name "Address" :type "string"}
                              {:name "Address Two" :type "string"}
                              {:name "City" :type "string"}
                              {:name "Postal Code" :type "string"}
                              {:name "State" :type "string"}
                              {:name "Country" :type "string"}
                              {:name "Lat" :type "decimal"}
                              {:name "Lng" :type "decimal"}])})

(def incubating
  [page view locale asset site domain location])

(defn spawn-models []
  (doall (map #(model/create :model %) incubating)))

(def migrate
  (fn []
    (spawn-models)))

