(ns triface.model
  (:use triface.debug)
  (:use triface.util)
  (:require [triface.db :as db]))

(defprotocol Field
  "a protocol for expected behavior of all model fields"
  (table-additions [this] "the set of additions to this db table based on the given name")
  (additional-processing [this] "further processing on creation of field")
  (field-from [this content opts] "retrieves the value for this field from this content item")
  (render [this content opts] "renders out a single field from this content item"))

(defrecord IntegerField [row env]
  Field
  (table-additions [this] [[(keyword (row :name)) :integer "DEFAULT 0"]])
  (additional-processing [this] nil)
  (field-from [this content opts] (content (keyword (row :name))))
  (render [this content opts] (field-from this content opts)))
  
(defrecord StringField [row env]
  Field
  (table-additions [this] [[(keyword (row :name)) "varchar(256)"]])
  (additional-processing [this] nil)
  (field-from [this content opts] (content (keyword (row :name))))
  (render [this content opts] (field-from this content opts)))

(defrecord TextField [row env]
  Field
  (table-additions [this] [[(keyword (row :name)) :text]])
  (additional-processing [this] nil)
  (field-from [this content opts] (content (keyword (row :name))))
  (render [this content opts] (field-from this content opts)))

(defrecord BooleanField [row env]
  Field
  (table-additions [this] [[(keyword (row :name)) :boolean]])
  (additional-processing [this] nil)
  (field-from [this content opts] (content (keyword (row :name))))
  (render [this content opts] (field-from this content opts)))

(defrecord TimestampField [row env]
  Field
  (table-additions [this] [[(keyword (row :name)) "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]])
  (additional-processing [this] nil)
  (field-from [this content opts] (content (keyword (row :name))))
  (render [this content opts] (str (field-from this content opts))))

;; forward reference for CollectionField
(def create-field)
(def model-render)
(def invoke-model)
(def models (ref {}))

(defn from [model content opts]
  (reduce #(assoc %1 (keyword (-> %2 :row :name)) (field-from %2 %1 opts)) content (vals (model :fields))))

(defrecord CollectionField [row env]
  Field
  (table-additions [this] [])

  (additional-processing [this]
    (let [model (db/choose :model (row :model_id))
          part (create-field {:name (:name model)
                    :type "part"
                    :model_id (row :target_id)
                    :target_id (row :model_id)
                    :link_id (row :id)})]
      (db/update :field {:link_id (-> part :row :id)} "id = %1" (row :id))))

  (field-from [this content opts]
    (let [include ((opts :include) (keyword (row :name)))]
      (if include
        (let [target (or (opts :target) (invoke-model (db/choose :model (row :target_id))))
              hole (dissoc (opts :include) (keyword (row :name)))
              down (assoc (dissoc opts :target) :include (merge hole include))
              link (db/choose :field (row :link_id))
              parts (db/fetch (target :slug) (str (link :name) "_id = %1") (content :id))]
          (map #(from target % down) parts))
        [])))

  (render [this content opts]
    (let [target (invoke-model (db/choose :model (row :target_id)))]
      (map #(model-render target % opts) (field-from this content (assoc opts :target target))))))

(defrecord PartField [row env]
  Field
  (table-additions [this] [[(keyword (str (row :name) "_id")) :integer "DEFAULT NULL"]
                           [(keyword (str (row :name) "_position")) :integer "DEFAULT 0"]])
  (additional-processing [this] nil)

  (field-from [this content opts]
    (let [include ((opts :include) (keyword (row :name)))]
      (if include
        (let [target (or (opts :target) (invoke-model (db/choose :model (row :target_id))))
              hole (dissoc (opts :include) (keyword (row :name)))
              down (assoc (dissoc opts :target) :include (merge hole include))
              collector (db/choose (target :slug) (content (keyword (str (row :name) "_id"))))]
          (from target collector down)))))

  (render [this content opts]
    (let [target (invoke-model (db/choose :model (row :target_id)))
          field (field-from this content (assoc opts :target target))]
      (if field
        (model-render target field opts)))))

(defrecord LinkField [row env]
  Field
  (table-additions [this] [])
  (additional-processing [this] nil)
  (field-from [this content opts])
  (render [this content opts] ""))

(def field-constructors
     {:integer (fn [row] (IntegerField. row {}))
      :string (fn [row] (StringField. row {}))
      :text (fn [row] (TextField. row {}))
      :boolean (fn [row] (BooleanField. row {}))
      :timestamp (fn [row] (TimestampField. row {}))
      :collection (fn [row] (CollectionField. row {}))
      :part (fn [row] (PartField. row {}))
      :link (fn [row] (LinkField. row {}))
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

(defn field-table-additions [fields]
  (reduce concat [] (map table-additions fields)))

(defn model-table-additions [model]
  (let [added (remove (set base-field-names) (keys (:fields model)))]
    (concat base-fields (field-table-additions (map #(% (:fields model)) added)))))

(defn fields-render [fields content opts]
  (reduce #(assoc %1 (keyword (-> %2 :row :name)) (render %2 content opts)) content fields))

(defn model-render [model content opts]
  (fields-render (vals (model :fields)) content opts))

(defn invoke-model [model]
  (let [fields (db/query "select * from field where model_id = %1" (model :id))
        field-map (seq-to-map #(keyword (-> % :row :name)) (map make-field fields))]
    (assoc model :fields field-map)))

(defn model-row-by-slug [table]
  (first (db/query "select * from model where name = '%1'" (name table))))

(defn model-for [slug]
  (invoke-model (model-row-by-slug slug)))

(defn invoke-models []
  (let [rows (db/query "select * from model")]
    (dosync
     (alter models merge
            (seq-to-map #(keyword (% :name)) (map invoke-model rows))))))

(defn create-model-table [name]
  (apply db/create-table
         (cons (keyword name)
               base-fields)))

(defn create-base-field [spec]
  (db/insert :field (assoc spec :slug (or (spec :slug) (spec :name))))
  (let [field-row (first (db/query "select * from field where name = '%1' and model_id = %2"
                                   (spec :name) (spec :model_id)))
        field (make-field field-row)]
    field))

(defn create-field [spec]
  (let [field (create-base-field spec)
        model (db/choose :model (spec :model_id))]
    (doall (map #(db/add-column (model :name) (name (first %)) (rest %)) (table-additions field)))
    field))

(defn add-fields [model specs]
  (let [fields (map #(create-field (assoc % :model_id (model :id))) specs)]
    ;; TODO: ensure linked models are refreshed as well
    (doall (map additional-processing fields))
    fields))

(defn create-model [spec]
  (db/insert :model (assoc (dissoc spec :fields) :slug (or (spec :slug) (spec :name))))
  (create-model-table (spec :name))
  (let [model (model-row-by-slug (spec :name))
        fields (concat (add-fields model (spec :fields))
                       (map #(create-base-field (assoc % :model_id (model :id))) base-rows))
        field-map (seq-to-map #(keyword (-> % :row :name)) fields)
        full-model (assoc model :fields field-map)]
    (dosync (alter models assoc (keyword (spec :name)) full-model))
    full-model))

(defn update-model [spec]
  '())

(defn delete-model [table]
  (let [model (model-row-by-slug table)]
    (db/delete :field "model_id = %1" (model :id))
    (db/delete :model "id = %1" (model :id))
    (db/drop-table (model :name))
    (dosync (alter models dissoc (keyword (model :name))))
    model))

(defn create-item [type spec]
  (db/insert type spec))



