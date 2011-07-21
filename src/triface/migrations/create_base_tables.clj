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
   [:name "varchar(55)" "NOT NULL" "UNIQUE"]
   [:description :text]
   [:position :integer]
   [:nested :boolean]
   [:locked :boolean]
   [:abstract :boolean]
   [:ancestor_id :integer]
   [:created_at "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]
   [:updated_at "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]))

(defn create-field-table []
  (db/create-table
   :field
   [:id "SERIAL" "PRIMARY KEY"]
   [:model_id :integer "DEFAULT 0"]
   [:link_id :integer "DEFAULT 0"]
   [:name "varchar(55)" "NOT NULL"]
   [:type "varchar(256)" "NOT NULL"]
   [:description :text]
   [:position :integer]
   [:required :boolean]
   [:disjoint :boolean]
   [:singular :boolean]
   [:locked :boolean]
   [:created_at "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]
   [:updated_at "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]))

(def migrate (fn []
  (create-migration-table)
  (create-model-table)
  (create-field-table)))

