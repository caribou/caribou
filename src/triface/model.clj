(ns triface.model
  (:use triface.debug)
  (:require [triface.db :as db]))

(defprotocol Field
  "a protocol for expected behavior of all model fields"
  (table-additions [this] "the set of additions to this db table based on the given name")
  (additional-processing [this] "further processing on creation of field")
  (include-by-default? [this] "whether or not to explicitly include this field in rendered output")
  (render [this content] "renders out a single field from this content item"))

(defrecord IntegerField [row]
  Field
  (table-additions [this] [[(keyword (row :name)) :integer "DEFAULT 0"]])
  (additional-processing [this] nil)
  (include-by-default? [this] true)
  (render [this content] (content (keyword (row :name)))))
  
(defrecord StringField [row]
  Field
  (table-additions [this] [[(keyword (row :name)) "varchar(256)"]])
  (additional-processing [this] nil)
  (include-by-default? [this] true)
  (render [this content] (content (keyword (row :name)))))

(defrecord TextField [row]
  Field
  (table-additions [this] [[(keyword (row :name)) :text]])
  (additional-processing [this] nil)
  (include-by-default? [this] true)
  (render [this content] (content (keyword (row :name)))))

(defrecord BooleanField [row]
  Field
  (table-additions [this] [[(keyword (row :name)) :boolean]])
  (additional-processing [this] nil)
  (include-by-default? [this] true)
  (render [this content] (content (keyword (row :name)))))

(defrecord TimestampField [row]
  Field
  (table-additions [this] [[(keyword (row :name)) "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]])
  (additional-processing [this] nil)
  (include-by-default? [this] true)
  (render [this content] (str (content (keyword (row :name))))))

;; forward reference for collection-field
(def create-field)

(defrecord CollectionField [row]
  Field
  (table-additions [this] [])
  (additional-processing [this]
   (let [model (db/choose :model (row :model_id))]
     (create-field {:name (:name model)
                    :type "belonging"
                    :model_id (:target_id row)
                    :link_id (:id row)})))
  (include-by-default? [this] false)
  (render [this content] ""))

(defrecord BelongingField [row]
  Field
  (table-additions [this] [[(keyword (str (row :name) "_id")) :integer "DEFAULT NULL"]
                           [(keyword (str (row :name) "_position")) :integer "DEFAULT 0"]])
  (additional-processing [this] nil)
  (include-by-default? [this] false)
  (render [this content] ""))

(defrecord LinkField [row]
  Field
  (table-additions [this] [])
  (additional-processing [this] nil)
  (include-by-default? [this] false)
  (render [this content] ""))

(def field-constructors
     {:integer (fn [row] (IntegerField. row))
      :string (fn [row] (StringField. row))
      :text (fn [row] (TextField. row))
      :boolean (fn [row] (BooleanField. row))
      :timestamp (fn [row] (TimestampField. row))
      :collection (fn [row] (CollectionField. row))
      :belonging (fn [row] (BelongingField. row))
      :link (fn [row] (LinkField. row))
      })

(def base-fields [[:id "SERIAL" "PRIMARY KEY"]
                  [:position :integer "DEFAULT 1"]
                  [:status :integer "DEFAULT 1"]
                  [:locale_id :integer "DEFAULT 1"]
                  [:env_id :integer "DEFAULT 1"]
                  [:locked :boolean "DEFAULT false"]
                  [:created_at "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]
                  [:updated_at "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]])

(def base-rows [{:name "id" :type "integer"}
                {:name "position" :type "integer"}
                {:name "status" :type "integer"}
                {:name "locale_id" :type "integer"}
                {:name "env_id" :type "integer"}
                {:name "locked" :type "boolean"}
                {:name "created_at" :type "timestamp"}
                {:name "updated_at" :type "timestamp"}])

(def base-field-names (map #(first %) base-fields))

(defn make-field [row]
  ((field-constructors (keyword (row :type))) row))

(defn seq-to-map [f q]
  (reduce #(assoc %1 (f %2) %2) {} q))

(defn field-table-additions [fields]
  (reduce concat [] (map table-additions fields)))

(defn model-table-additions [model]
  (let [added (remove (set base-field-names) (keys (:fields model)))]
    (concat base-fields (field-table-additions (map #(% (:fields model)) added)))))

(defn model-render [model content]
  (reduce #(assoc %1 (keyword (:name (:row %2))) (render %2 content)) content (vals (model :fields))))

(defn model-by-name [name]
  (first (db/query "select * from model where name = '%1'" name)))

(def models (ref {}))

(defn invoke-model [model]
  (let [fields (db/query "select * from field where model_id = %1" (model :id))
        field-map (seq-to-map #(keyword (:name (:row %))) (map make-field fields))]
    (assoc model :fields field-map)))

(defn invoke-models []
  (dosync
   (alter models
          merge
          (reduce #(assoc %1 (keyword (%2 :name)) (invoke-model %2)) {}
                  (db/query "select * from model")))))

(defn create-model-table [name]
  (apply db/create-table
         (cons (keyword name)
               base-fields)))

(defn create-base-field [spec]
  (db/insert :field spec)
  (let [field-row (first (db/query "select * from field where name = '%1' and model_id = %2"
                                   (spec :name) (spec :model_id)))
        field (make-field field-row)]
    field))

(defn create-field [spec]
  (let [field (create-base-field spec)
        model (db/choose :model (spec :model_id))]
    (doall (map #(db/add-column (model :name) (name (first %)) (rest %)) (table-additions field)))
    field))

(defn create-model [spec]
  (db/insert :model (dissoc spec :fields))
  (create-model-table (:name spec))
  (let [model (model-by-name (:name spec))
        fields (concat (map #(create-field (assoc % :model_id (model :id))) (spec :fields))
                       (map #(create-base-field (assoc % :model_id (model :id))) base-rows))
        field-map (seq-to-map #(keyword (:name (:row %))) fields)
        full-model (assoc model :fields field-map)]
    (map additional-processing fields)
    (dosync (alter models assoc (keyword (spec :name)) full-model))
    full-model))

(defn update-model [spec]
  '())

(defn delete-model [name]
  (let [model (model-by-name name)]
    (db/delete :field "model_id = %1" (model :id))
    (db/delete :model "id = %1" (model :id))
    (db/drop-table (model :name))
    (dosync (alter models dissoc (keyword (model :name))))
    model))

(defn create-item [type spec]
  (db/insert type spec))



