(ns triface.model
  (:use triface.debug)
  (:use triface.util)
  (:require [triface.db :as db]))

(defprotocol Field
  "a protocol for expected behavior of all model fields"
  (table-additions [this] "the set of additions to this db table based on the given name")
  (setup-field [this] "further processing on creation of field")
  (cleanup-field [this] "further processing on creation of field")
  (target-for [this] "retrieves the model this field points to, if applicable")
  (update-values [this content values] "adds to the map of values that will be committed to the db")
  (field-from [this content opts] "retrieves the value for this field from this content item")
  (render [this content opts] "renders out a single field from this content item"))

(defrecord IntegerField [row env]
  Field
  (table-additions [this] [[(keyword (row :name)) :integer "DEFAULT 0"]])
  (setup-field [this] nil)
  (cleanup-field [this] nil)
  (target-for [this] nil)

  (update-values [this content values]
    (let [key (keyword (row :name))]
      (if (contains? content key)
        (try
          (let [value (content key)
                tval (if (isa? (type value) String)
                       (Integer/parseInt value)
                       value)]
            (assoc values key tval))
          (catch Exception e values))
        values)))

  (field-from [this content opts] (content (keyword (row :name))))
  (render [this content opts] (field-from this content opts)))
  
(defrecord StringField [row env]
  Field
  (table-additions [this] [[(keyword (row :name)) "varchar(256)"]])
  (setup-field [this] nil)
  (cleanup-field [this] nil)
  (target-for [this] nil)
  (field-from [this content opts] (content (keyword (row :name))))
  (update-values [this content values]
    (let [key (keyword (row :name))]
      (if (contains? content key)
        (assoc values key (content key))
        values)))
  (render [this content opts] (field-from this content opts)))

(defrecord SlugField [row env]
  Field
  (table-additions [this] [[(keyword (row :name)) "varchar(256)"]])
  (setup-field [this] nil)
  (cleanup-field [this] nil)
  (target-for [this] nil)
  (field-from [this content opts] (content (keyword (row :name))))
  (update-values [this content values]
    (let [key (keyword (row :name))]
      (if (contains? content key)
        (assoc values key (content key))
        values)))
  (render [this content opts] (field-from this content opts)))

(defrecord TextField [row env]
  Field
  (table-additions [this] [[(keyword (row :name)) :text]])
  (setup-field [this] nil)
  (cleanup-field [this] nil)
  (target-for [this] nil)
  (field-from [this content opts] (content (keyword (row :name))))
  (update-values [this content values]
    (let [key (keyword (row :name))]
      (if (contains? content key)
        (assoc values key (content key))
        values)))
  (render [this content opts] (field-from this content opts)))

(defrecord BooleanField [row env]
  Field
  (table-additions [this] [[(keyword (row :name)) :boolean]])
  (setup-field [this] nil)
  (cleanup-field [this] nil)
  (target-for [this] nil)
  (field-from [this content opts] (content (keyword (row :name))))
  (update-values [this content values]
    (let [key (keyword (row :name))]
      (if (contains? content key)
        (try
          (let [value (content key)
                tval (if (isa? (type value) String)
                       (Boolean/parseBoolean value)
                       value)]
            (assoc values key tval))
          (catch Exception e values))
        values)))
  (render [this content opts] (field-from this content opts)))

(defrecord TimestampField [row env]
  Field
  (table-additions [this] [[(keyword (row :name)) "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]])
  (setup-field [this] nil)
  (cleanup-field [this] nil)
  (target-for [this] nil)
  (field-from [this content opts] (content (keyword (row :name))))
  (update-values [this content values]
    (let [key (keyword (row :name))]
      (cond
       (= key :updated_at) (assoc values key :current_timestamp)
       (contains? content key) (assoc values key (content key))
       :else values)))
  (render [this content opts] (str (field-from this content opts))))

;; forward reference for CollectionField
(def make-field)
(def create-field)
(def add-fields)
(def destroy-field)
(def model-render)
(def invoke-model)
(def models (ref {}))

(defn from [model content opts]
  (reduce #(assoc %1 (keyword (-> %2 :row :name)) (field-from %2 %1 opts)) content (vals (model :fields))))

(defrecord CollectionField [row env]
  Field
  (table-additions [this] [])

  (setup-field [this]
    (if (or (nil? (row :link_id)) (zero? (row :link_id)))
      (let [model (db/choose :model (row :model_id))
            part (create-field {:name (:name model)
                                :type "part"
                                :model_id (row :target_id)
                                :target_id (row :model_id)
                                :link_id (row :id)})]
        (setup-field part)
        (db/update :field {:link_id (-> part :row :id)} "id = %1" (row :id)))))

  (cleanup-field [this]
    (destroy-field (make-field (-> env :link))))

  (target-for [this] (models (-> this :row :target_id)))

  (field-from [this content opts]
    (let [include (if (opts :include) ((opts :include) (keyword (row :name))))]
      (if include
        (let [hole (dissoc (opts :include) (keyword (row :name)))
              down (assoc opts :include (merge hole include))
              parts (db/fetch (-> (target-for this) :slug) (str (-> this :env :link :slug) "_id = %1") (content :id))]
          (map #(from (target-for this) % down) parts))
        [])))

  (update-values [this content values] values)

  (render [this content opts]
    (map #(model-render (target-for this) % opts) (field-from this content opts))))

(defrecord PartField [row env]
  Field

  (table-additions [this] [])

  (setup-field [this]
    (let [model_id (-> this :row :model_id)
          model (models model_id)]
      (add-fields model [{:name (str (row :name) "_id") :type "integer" :editable false :model_id model_id}
                         {:name (str (row :name) "_position") :type "integer" :editable false :model_id model_id}])))

  (cleanup-field [this]
    (destroy-field (make-field (-> env :link))))

  (target-for [this] (models (-> this :row :target_id)))

  (field-from [this content opts]
    (let [include (if (opts :include) ((opts :include) (keyword (row :name))))]
      (if include
        (let [hole (dissoc (opts :include) (keyword (row :name)))
              down (assoc opts :include (merge hole include))
              collector (db/choose (-> (target-for this) :slug) (content (keyword (str (row :name) "_id"))))]
          (from (target-for this) collector down)))))

  (update-values [this content values] values)

  (render [this content opts]
    (let [field (field-from this content opts)]
      (if field
        (model-render (target-for this) field opts)))))

(defrecord LinkField [row env]
  Field
  (table-additions [this] [])
  (setup-field [this] nil)
  (cleanup-field [this] nil)
  (target-for [this] nil)
  (field-from [this content opts])
  (update-values [this content values])
  (render [this content opts] ""))

(def field-constructors
     {:integer (fn [row] (IntegerField. row {}))
      :string (fn [row] (StringField. row {}))
      :text (fn [row] (TextField. row {}))
      :boolean (fn [row] (BooleanField. row {}))
      :timestamp (fn [row] (TimestampField. row {}))
      :collection (fn [row]
                    (let [link (if (row :link_id) (db/choose :field (row :link_id)))]
                      (CollectionField. row {:link link})))
      :part (fn [row]
              (let [link (db/choose :field (row :link_id))]
                (PartField. row {:link link})))
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

(def base-rows [{:name "id" :type "integer" :locked true :immutable true :editable false}
                {:name "position" :type "integer" :locked true}
                {:name "status" :type "integer" :locked true}
                {:name "locale_id" :type "integer" :locked true :editable false}
                {:name "env_id" :type "integer" :locked true :editable false}
                {:name "locked" :type "boolean" :locked true :immutable true :editable false}
                {:name "created_at" :type "timestamp" :locked true :immutable true :editable false}
                {:name "updated_at" :type "timestamp" :locked true :editable false}])

(def base-field-names (map #(first %) base-fields))

(defn make-field [row]
  ((field-constructors (keyword (row :type))) row))

(defn field-table-additions [fields]
  (reduce concat [] (map table-additions fields)))

(defn model-table-additions [model]
  (let [added (remove (set base-field-names) (keys (model :fields)))]
    (concat base-fields (field-table-additions (map #(% (model :fields)) added)))))

(defn fields-render [fields content opts]
  (reduce #(assoc %1 (keyword (-> %2 :row :name)) (render %2 content opts)) content fields))

(defn model-render [model content opts]
  (fields-render (vals (model :fields)) content opts))

(defn invoke-model [model]
  (let [fields (db/query "select * from field where model_id = %1" (model :id))
        field-map (seq-to-map #(keyword (-> % :row :name)) (map make-field fields))]
    (assoc model :fields field-map)))

(defn model-row-by-slug [table]
  (first (db/query "select * from model where slug = '%1'" (name table))))

(defn model-for [slug]
  (invoke-model (model-row-by-slug slug)))

(defn invoke-models []
  (let [rows (db/query "select * from model")
        invoked (map invoke-model rows)]
    (dosync
     (alter models 
            (fn [in-ref new-models] new-models)
            (merge (seq-to-map #(keyword (% :name)) invoked)
                   (seq-to-map #(% :id) invoked))))))

(defn create-model-table [name]
  (apply db/create-table
         (cons (keyword name)
               base-fields)))

(defn create-base-field [spec]
  (let [field-row (db/insert :field (assoc spec :slug (or (spec :slug) (spec :name))))
        field (make-field field-row)]
    field))

(defn create-field [spec]
  (let [field (create-base-field spec)
        model (db/choose :model (spec :model_id))]
    (doall (map #(db/add-column (model :name) (name (first %)) (rest %)) (table-additions field)))
    field))

(defn add-fields [model specs]
  (let [fields (map #(create-field (assoc % :model_id (model :id))) specs)]
    (doall (map setup-field fields))
    fields))

(defn create-model [spec]
  (db/insert :model (assoc (dissoc spec :fields) :slug (or (spec :slug) (spec :name))))
  (create-model-table (spec :name))
  (let [model (model-row-by-slug (spec :name))
        fields (concat
                (add-fields model (spec :fields))
                (doall (map #(create-base-field (assoc % :model_id (model :id))) base-rows)))]
    (invoke-models)
    (models (keyword (model :slug)))))

(defn destroy-field [field]
  (doall (map #(db/drop-column ((models (-> field :row :model_id)) :slug) (first %)) (table-additions field)))
  (db/delete :field "id = %1" (-> field :row :id)))

(defn remove-fields [fields]
  (doall (map cleanup-field fields))
  (doall (map destroy-field fields)))

(defn update-model [spec]
  '())

(defn delete-model [slug]
  (let [model (model-for (keyword slug))]
    (remove-fields (vals (model :fields)))
    (db/drop-table (model :slug))
    (db/delete :model "id = %1" (model :id))
    (invoke-models)
    model))

(defn create-content [slug spec]
  (let [model (models (keyword slug))
        values (reduce #(update-values %2 spec %1) {} (vals (model :fields)))]
    (db/insert slug values)))

(defn update-content [slug id spec]
  (let [model (models (keyword slug))
        values (reduce #(update-values %2 spec %1) {} (vals (model :fields)))]
    (db/update slug values "id = %1" id)
    (assoc spec :id id)))



