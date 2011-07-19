(in-ns 'triface.migration)

(defn create-migration-table []
  (db/create-table
   :migration
   [:id "SERIAL" "PRIMARY KEY"]
   [:name "varchar(55)" "NOT NULL" "UNIQUE"]
   [:run_at "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]))

(defn create-model-table []
  (db/create-table
   :model
   [:id "SERIAL" "PRIMARY KEY"]
   [:name "varchar(55)"]
   [:description :text]
   [:position :integer]
   [:nested :boolean]   [:locked :boolean]
   [:abstract :boolean]
   [:ancestor_id :integer]
   [:created_at "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]
   [:updated_at "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"])
  (db/insert
   :model
   {:name "model"
    :description "base model for models"
    :position 1
    :nested false
    :locked true
    :abstract false
    :ancestor_id 0}))

(def migrate (fn []
  (create-migration-table)
  (create-model-table)))

