(ns triface.model
  (:require [triface.db :as db]))

(defprotocol Field
  "a protocol for expected behavior of all model fields"
  (table-additions [this] "the set of additions to this db table based on the given name")
  (render [this content] "renders out a single field from this content item"))

(defrecord IntegerField [row]
  Field
  (table-additions [this] [[(keyword (row :name)) :integer "DEFAULT 0"]])
  (render [this content] (content (keyword (row :name)))))
  
(defrecord StringField [row]
  Field
  (table-additions [this] [[(keyword (row :name)) "varchar(256)"]])
  (render [this content] (content (keyword (row :name)))))

(defrecord TextField [row]
  Field
  (table-additions [this] [[(keyword (row :name)) :text]])
  (render [this content] (content (keyword (row :name)))))

(defrecord BooleanField [row]
  Field
  (table-additions [this] [[(keyword (row :name)) :boolean]])
  (render [this content] (content (keyword (row :name)))))

(defrecord TimestampField [row]
  Field
  (table-additions [this] [[(keyword (row :name)) "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]])
  (render [this content] (str (content (keyword (row :name))))))

(defrecord LinkField [row]
  Field
  (table-additions [this] [])
  (render [this content] ""))

(def field-constructors
     {:integer (fn [row] (IntegerField. row))
      :string (fn [row] (StringField. row))
      :text (fn [row] (TextField. row))
      :boolean (fn [row] (BooleanField. row))
      :timestamp (fn [row] (TimestampField. row))})

(defn make-field [row]
  ((field-constructors (keyword (row :type))) row))

(defn rows-to-fields [rows]
  (reduce #(assoc %1 (keyword (%2 :name)) (make-field %2)) {} rows))

(defn invoke-field-records [model rows]
  (assoc model :fields (rows-to-fields rows)))

(defn fetch-fields [model]
  (invoke-field-records model (db/query "select * from field where model_id = %1" (model :id))))

(defn define-fields [model]
  (invoke-field-records model (model :fields)))

(defn model-by-name [name]
  (first (db/query "select * from model where name = '%1'" name)))

(defn fetch-model [name]
  (fetch-fields (model-by-name name)))

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

(defn field-table-additions [model]
  (reduce #(concat %1 (table-additions %2)) [] (vals (model :fields))))

(defn model-table-additions [model]
  (concat base-fields (field-table-additions model)))

(defn model-render [model content]
  (reduce #(assoc %1 %2 (render ((model :fields) %2) content)) content (keys (model :fields))))

(defn create-model-table [model]
  (apply db/create-table
         (cons (keyword (model :name))
               (model-table-additions model))))

(def models (ref {}))

(defn create-model [spec]
  (db/insert :model (dissoc spec :fields))
  (let [model (model-by-name (spec :name))]
    (doall (map #(db/insert :field (assoc % :model_id (model :id)))
                (concat (spec :fields) base-rows)))
    (create-model-table (define-fields spec))
    (let [model (fetch-fields model)]
      (dosync (alter models assoc (keyword (spec :name)) model))
      model)))

(defn delete-model [name]
  (let [model (fetch-model name)]
    (db/delete :field "model_id = %1" (model :id))
    (db/delete :model "id = %1" (model :id))
    (db/drop-table (model :name))
    (dosync (alter models dissoc (keyword (model :name))))
    model))

(defn create-item [type spec]
  (db/insert type spec))



