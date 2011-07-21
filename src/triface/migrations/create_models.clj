(in-ns 'triface.migration)

(defn create-model-model []
  (db/insert
   :model
   {:name "model"
    :description "base model for models"
    :position 1
    :nested false
    :locked true
    :abstract false
    :ancestor_id 0}))

(defn create-model-fields []
  (let [model-id ((first (db/query "select id from model where name = 'model'")) :id)]
    (db/insert
     :field
     {:name "id"
      :type "integer"
      :model_id model-id})
    (db/insert
     :field
     {:name "name"
      :type "string"
      :model_id model-id})
    (db/insert
     :field
     {:name "description"
      :type "text"
      :model_id model-id})
    (db/insert
     :field
     {:name "position"
      :type "integer"
      :model_id model-id})
    (db/insert
     :field
     {:name "nested"
      :type "boolean"
      :model_id model-id})
    (db/insert
     :field
     {:name "locked"
      :type "boolean"
      :model_id model-id})
    (db/insert
     :field
     {:name "abstract"
      :type "boolean"
      :model_id model-id})
    (db/insert
     :field
     {:name "ancestor_id"
      :type "integer"
      :model_id model-id})
    (db/insert
     :field
     {:name "created_at"
      :type "timestamp"
      :model_id model-id})
    (db/insert
     :field
     {:name "updated_at"
      :type "timestamp"
      :model_id model-id})))

(defn create-field-model []
  (db/insert
   :model
   {:name "field"
    :description "a model that specifies what fields a model has"
    :position 2
    :nested false
    :locked true
    :abstract false
    :ancestor_id 0}))

(defn create-field-fields []
  (let [model-id ((first (db/query "select id from model where name = 'field'")) :id)]
    (db/insert
     :field
     {:name "id"
      :type "integer"
      :model_id model-id})
    (db/insert
     :field
     {:name "model_id"
      :type "integer"
      :model_id model-id})
    (db/insert
     :field
     {:name "link_id"
      :type "integer"
      :model_id model-id})
    (db/insert
     :field
     {:name "name"
      :type "string"
      :model_id model-id})
    (db/insert
     :field
     {:name "type"
      :type "string"
      :model_id model-id})
    (db/insert
     :field
     {:name "description"
      :type "text"
      :model_id model-id})
    (db/insert
     :field
     {:name "position"
      :type "integer"
      :model_id model-id})
    (db/insert
     :field
     {:name "required"
      :type "boolean"
      :model_id model-id})
    (db/insert
     :field
     {:name "disjoint"
      :type "boolean"
      :model_id model-id})
    (db/insert
     :field
     {:name "singular"
      :type "boolean"
      :model_id model-id})
    (db/insert
     :field
     {:name "locked"
      :type "boolean"
      :model_id model-id})
    (db/insert
     :field
     {:name "created_at"
      :type "timestamp"
      :model_id model-id})
    (db/insert
     :field
     {:name "updated_at"
      :type "timestamp"
      :model_id model-id})))

(def migrate (fn []
  (create-model-model)
  (create-model-fields)
  (create-field-model)
  (create-field-fields)))

