(in-ns 'triface.migration)
(use 'triface.model)

(defn create-model-model []
  (db/insert
   :model
   {:name "model"
    :slug "model"
    :description "base model for models"
    :position 1
    :locked true}))

(defn create-field-model []
  (db/insert
   :model
   {:name "field"
    :slug "field"
    :description "a model that specifies what fields a model has"
    :position 2
    :locked true}))

(defn create-model-fields []
  (let [model-id ((first (db/query "select id from model where name = 'model'")) :id)]
    (db/insert
     :field
     {:name "id"
      :slug "id"
      :type "integer"
      :model_id model-id})
    (db/insert
     :field
     {:name "name"
      :slug "name"
      :type "string"
      :model_id model-id})
    (db/insert
     :field
     {:name "slug"
      :slug "slug"
      :type "string"
      :model_id model-id})
    (db/insert
     :field
     {:name "description"
      :slug "description"
      :type "text"
      :model_id model-id})
    (db/insert
     :field
     {:name "position"
      :slug "position"
      :type "integer"
      :model_id model-id})
    (db/insert
     :field
     {:name "nested"
      :slug "nested"
      :type "boolean"
      :model_id model-id})
    (db/insert
     :field
     {:name "fields"
      :slug "fields"
      :type "collection"
      :model_id model-id})
    (db/insert
     :field
     {:name "locked"
      :slug "locked"
      :type "boolean"
      :model_id model-id})
    (db/insert
     :field
     {:name "abstract"
      :slug "abstract"
      :type "boolean"
      :model_id model-id})
    (db/insert
     :field
     {:name "ancestor_id"
      :slug "ancestor_id"
      :type "integer"
      :model_id model-id})
    (db/insert
     :field
     {:name "created_at"
      :slug "created_at"
      :type "timestamp"
      :model_id model-id})
    (db/insert
     :field
     {:name "updated_at"
      :slug "updated_at"
      :type "timestamp"
      :model_id model-id})))

(defn create-field-fields []
  (let [model-id ((first (db/query "select id from model where name = 'field'")) :id)]
    (db/insert
     :field
     {:name "id"
      :slug "id"
      :type "integer"
      :model_id model-id})
    (db/insert
     :field
     {:name "link_id"
      :slug "link_id"
      :type "integer"
      :model_id model-id})
    (db/insert
     :field
     {:name "name"
      :slug "name"
      :type "string"
      :model_id model-id})
    (db/insert
     :field
     {:name "slug"
      :slug "slug"
      :type "string"
      :model_id model-id})
    (db/insert
     :field
     {:name "type"
      :slug "type"
      :type "string"
      :model_id model-id})
    (db/insert
     :field
     {:name "description"
      :slug "description"
      :type "text"
      :model_id model-id})
    (db/insert
     :field
     {:name "position"
      :slug "position"
      :type "integer"
      :model_id model-id})
    (db/insert
     :field
     {:name "model"
      :slug "model"
      :type "part"
      :model_id model-id})
    (db/insert
     :field
     {:name "required"
      :slug "required"
      :type "boolean"
      :model_id model-id})
    (db/insert
     :field
     {:name "disjoint"
      :slug "disjoint"
      :type "boolean"
      :model_id model-id})
    (db/insert
     :field
     {:name "singular"
      :slug "singular"
      :type "boolean"
      :model_id model-id})
    (db/insert
     :field
     {:name "locked"
      :slug "locked"
      :type "boolean"
      :model_id model-id})
    (db/insert
     :field
     {:name "created_at"
      :slug "created_at"
      :type "timestamp"
      :model_id model-id})
    (db/insert
     :field
     {:name "updated_at"
      :slug "updated_at"
      :type "timestamp"
      :model_id model-id})))

(defn forge-link []
  (let [model (first (db/fetch :model "name = '%1'" "model"))
        field (first (db/fetch :model "name = '%1'" "field"))
        collection (first (db/fetch :field "name = '%1' and model_id = %2" "fields" (model :id)))
        part (first (db/fetch :field "name = '%1' and model_id = %2" "model" (field :id)))]
    (db/update :field {:link_id (collection :id) :target_id (model :id)} "id = %1" (part :id))
    (db/update :field {:link_id (part :id) :target_id (field :id)} "id = %1" (collection :id))))

(def migrate (fn []
  (create-model-model)
  (create-model-fields)
  (create-field-model)
  (create-field-fields)
  (forge-link)))

