(ns triface.model
  (require [triface.db :as db]))

(defprotocol Field
  "a protocol for expected behavior of all model fields"
  (table-additions [this] "the set of additions to this db table based on the given name")
  (render [this content]) "renders out a single field from this content item")

(defrecord IntegerField [row]
  Field
  (table-additions [this] [[(keyword (row :name)) :integer "DEFAULT 0"]])
  (render [this content] (str (content (keyword (row :name))))))
  
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
  (render [this content] (str (content (keyword (row :name))))))

(defrecord TimestampField [row]
  Field
  (table-additions [this] [[(keyword (row :name)) "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]])
  (render [this content] (str (content (keyword (row :name))))))

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

(defn fetch-fields [model]
  (assoc model :fields (rows-to-fields (db/query "select * from field where model_id = %1" (str (model :id))))))

(defn fetch-model [name]
  (fetch-fields (first (db/query "select * from model where name = '%1'" name))))

(defn model-table-additions [model]
  (reduce #(concat %1 (table-additions %2)) [] (vals (model :fields))))

(defn model-render [model content]
  (reduce #(assoc %1 %2 (render ((model :fields) %2) content)) content (keys (model :fields))))

(def base-fields [[:id "SERIAL" "PRIMARY KEY"]
                  [:position :integer "DEFAULT 1"]
                  [:status :integer "DEFAULT 0"]
                  [:locked :boolean "DEFAULT false"]
                  [:created_at "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]
                  [:updated_at "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]])

(defn create-model-table [model]
  (apply db/create-table
         (cons (keyword (model :name))
               (concat base-fields (model-table-additions model)))))

(defn create-model [base fields])

(def models (ref (reduce #(assoc %1 (keyword (%2 :name)) (fetch-fields %2)) {}
                         (db/query "select * from model"))))

