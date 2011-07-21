(ns triface.model
  (require [triface.db :as db]))

(defprotocol Field
  "a protocol for expected behavior of all model fields"
  (table-additions [this] "the set of additions to this db table based on the given name"))

(defrecord IntegerField [row]
  Field
  (table-additions [this] [[(keyword (row :name)) :integer "DEFAULT 0"]]))
  
(defrecord StringField [row]
  Field
  (table-additions [this] [[(keyword (row :name)) "varchar(256)"]]))

(defrecord TextField [row]
  Field
  (table-additions [this] [[(keyword (row :name)) :text]]))

(defrecord BooleanField [row]
  Field
  (table-additions [this] [[(keyword (row :name)) :boolean]]))

(defrecord TimestampField [row]
  Field
  (table-additions [this] [[(keyword (row :name)) "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]]))

;; (defrecord ReferenceField [row reference delete]
;;   Field
;;   (table-additions [this] [[(keyword (str (row :name) "_id")) (str "REFERENCES " reference) (str "ON DELETE " delete)]]))

(def field-types
     {"integer" IntegerField
      "string" StringField
      "text" TextField
      "boolean" BooleanField
      "reference" ReferenceField})

(defn fetch-model [name]
  (let [row (first (db/query "select * from model where name = %1" name))
        fields (db/query "select * from field where model_id = %1" (row :id))]
    (assoc row :fields (map #(field-types (% :type)) fields))))

(def base-fields [[:id "SERIAL" "PRIMARY KEY"]
                  [:position :integer "DEFAULT 1"]
                  [:status :integer "DEFAULT 0"]
                  [:locked :boolean "DEFAULT false"]
                  [:created_at "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]
                  [:updated_at "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]])

; fields

(defn create-model-table [model]
  (apply db/create-table
         (cons (keyword (model :name))
               (concat base-fields (reduce #(concat %1 (table-additions %2)) [] (model :fields))))))

(defn create-model [base fields])

(def models (ref []))