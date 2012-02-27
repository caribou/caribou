(require '[caribou.db :as db])
(require '[caribou.model :as model])

(defn create-model-model []
  (db/insert
   :model
   {:name "Model"
    :slug "model"
    :description "base model for models"
    :position 1
    :locked true}))

(defn create-field-model []
  (db/insert
   :model
   {:name "Field"
    :slug "field"
    :description "a model that specifies what fields a model has"
    :position 2
    :locked true}))

(defn create-model-fields []
  (let [model-id ((first (db/query "select id from model where slug = 'model'")) :id)]
    (db/insert
     :field
     {:name "Id"
      :slug "id"
      :type "integer"
      :locked true
      :immutable true
      :editable false
      :model_id model-id})
    (let [name-field (db/insert
                      :field
                      {:name "Name"
                       :slug "name"
                       :type "string"
                       :locked true
                       :model_id model-id})]
      (db/insert
       :field
       {:name "Slug"
        :slug "slug"
        :type "slug"
        :locked true
        :editable false
        :link_id (name-field :id)
        :model_id model-id}))
    (db/insert
     :field
     {:name "Description"
      :slug "description"
      :type "text"
      :locked true
      :model_id model-id})
    (db/insert
     :field
     {:name "Position"
      :slug "position"
      :type "integer"
      :locked true
      :model_id model-id})
    (db/insert
     :field
     {:name "Nested"
      :slug "nested"
      :type "boolean"
      :locked true
      :model_id model-id})
    (db/insert
     :field
     {:name "Join Model"
      :slug "join_model"
      :type "boolean"
      :locked true
      :model_id model-id})
    (db/insert
     :field
     {:name "Fields"
      :slug "fields"
      :type "collection"
      :dependent true
      :locked true
      :model_id model-id})
    (db/insert
     :field
     {:name "Locked"
      :slug "locked"
      :type "boolean"
      :locked true
      :immutable true
      :editable false
      :model_id model-id})
    (db/insert
     :field
     {:name "Abstract"
      :slug "abstract"
      :type "boolean"
      :locked true
      :model_id model-id})
    (db/insert
     :field
     {:name "Searchable"
      :slug "searchable"
      :type "boolean"
      :locked true
      :model_id model-id})
    (db/insert
     :field
     {:name "Ancestor Id"
      :slug "ancestor_id"
      :type "integer"
      :locked true
      :editable false
      :model_id model-id})
    (db/insert
     :field
     {:name "Created At"
      :slug "created_at"
      :type "timestamp"
      :locked true
      :immutable true
      :editable false
      :model_id model-id})
    (db/insert
     :field
     {:name "Updated At"
      :slug "updated_at"
      :type "timestamp"
      :locked true
      :editable false
      :model_id model-id})))

(defn create-field-fields []
  (let [model-id ((first (db/query "select id from model where slug = 'field'")) :id)]
    (db/insert
     :field
     {:name "Id"
      :slug "id"
      :type "integer"
      :locked true
      :immutable true
      :editable false
      :model_id model-id})
    (db/insert
     :field
     {:name "Link"
      :slug "link"
      :type "tie"
      :locked true
      :immutable true
      :editable false
      :model_id model-id})
    (db/insert
     :field
     {:name "Link Id"
      :slug "link_id"
      :type "integer"
      :locked true
      :editable false
      :model_id model-id})
    (let [name-field (db/insert
                      :field
                      {:name "Name"
                       :slug "name"
                       :type "string"
                       :locked true
                       :model_id model-id})]
      (db/insert
       :field
       {:name "Slug"
        :slug "slug"
        :type "slug"
        :locked true
        :editable false
        :link_id (name-field :id)
        :model_id model-id}))
    (db/insert
     :field
     {:name "Type"
      :slug "type"
      :type "string"
      :locked true
      :immutable true
      :editable false
      :model_id model-id})
    (db/insert
     :field
     {:name "Default Value"
      :slug "default_value"
      :type "string"
      :locked true
      :immutable true
      :editable false
      :model_id model-id})
    (db/insert
     :field
     {:name "Description"
      :slug "description"
      :type "text"
      :locked true
      :model_id model-id})
    (db/insert
     :field
     {:name "Position"
      :slug "position"
      :type "integer"
      :locked true
      :model_id model-id})
    (db/insert
     :field
     {:name "Model Id"
      :slug "model_id"
      :type "integer"
      :locked true
      :editable false
      :model_id model-id})
    (db/insert
     :field
     {:name "Target Id"
      :slug "target_id"
      :type "integer"
      :locked true
      :editable false
      :model_id model-id})
    (db/insert
     :field
     {:name "Model Position"
      :slug "model_position"
      :type "integer"
      :locked true
      :editable false
      :model_id model-id})
    (db/insert
     :field
     {:name "Model"
      :slug "model"
      :type "part"
      :locked true
      :dependent true
      :model_id model-id})
    (db/insert
     :field
     {:name "Required"
      :slug "required"
      :type "boolean"
      :locked true
      :model_id model-id})
    (db/insert
     :field
     {:name "Disjoint"
      :slug "disjoint"
      :type "boolean"
      :locked true
      :model_id model-id})
    (db/insert
     :field
     {:name "Singular"
      :slug "singular"
      :type "boolean"
      :locked true
      :model_id model-id})
    (db/insert
     :field
     {:name "Editable"
      :slug "editable"
      :type "boolean"
      :locked true
      :immutable true
      :editable false
      :model_id model-id})
    (db/insert
     :field
     {:name "Immutable"
      :slug "immutable"
      :type "boolean"
      :locked true
      :immutable true
      :editable false
      :model_id model-id})
    (db/insert
     :field
     {:name "Locked"
      :slug "locked"
      :type "boolean"
      :locked true
      :immutable true
      :editable false
      :model_id model-id})
    (db/insert
     :field
     {:name "Searchable"
      :slug "searchable"
      :type "boolean"
      :locked true
      :model_id model-id})
    (db/insert
     :field
     {:name "Dependent"
      :slug "dependent"
      :type "boolean"
      :locked true
      :model_id model-id})
    (db/insert
     :field
     {:name "Created At"
      :slug "created_at"
      :type "timestamp"
      :locked true
      :immutable true
      :editable false
      :model_id model-id})
    (db/insert
     :field
     {:name "Updated At"
      :slug "updated_at"
      :type "timestamp"
      :locked true
      :editable false
      :model_id model-id})))

(defn forge-link []
  (let [model (first (db/fetch :model "slug = '%1'" "model"))
        field (first (db/fetch :model "slug = '%1'" "field"))
        collection (first (db/fetch :field "slug = '%1' and model_id = %2" "fields" (model :id)))
        part (first (db/fetch :field "slug = '%1' and model_id = %2" "model" (field :id)))]
    (db/update :field ["id = ?" (part :id)] {:link_id (collection :id) :target_id (model :id)})
    (db/update :field ["id = ?" (collection :id)] {:link_id (part :id) :target_id (field :id)})))

(defn migrate
  []
  (create-model-model)
  (create-model-fields)
  (create-field-model)
  (create-field-fields)
  (forge-link))

(migrate)