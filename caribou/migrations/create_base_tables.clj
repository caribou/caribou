(require '[caribou.db :as db])

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
   [:slug "varchar(55)" "NOT NULL" "UNIQUE"]
   [:description :text "DEFAULT ''"]
   [:position :integer "DEFAULT 0"]
   [:nested :boolean "DEFAULT false"]
   [:locked :boolean "DEFAULT false"]
   [:join_model :boolean "DEFAULT false"]
   [:abstract :boolean "DEFAULT false"]
   [:searchable :boolean "DEFAULT false"]
   [:ancestor_id :integer "DEFAULT NULL"]
   [:created_at "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]
   [:updated_at "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]))

(defn create-field-table []
  (db/create-table
   :field
   [:id "SERIAL" "PRIMARY KEY"]
   [:name "varchar(55)" "NOT NULL"]
   [:slug "varchar(55)" "NOT NULL"]
   [:type "varchar(256)" "NOT NULL"]
   [:default_value "varchar(256)"]
   [:link_id :integer "DEFAULT NULL"]
   [:model_id :integer "NOT NULL"]
   [:model_position :integer "DEFAULT 0"]
   [:target_id :integer "DEFAULT NULL"]
   [:target_type "varchar(55)" "DEFAULT NULL"]
   [:description :text "DEFAULT ''"]
   [:position :integer "DEFAULT 0"]
   [:required :boolean "DEFAULT false"]
   [:disjoint :boolean "DEFAULT false"]
   [:singular :boolean "DEFAULT false"]
   [:locked :boolean "DEFAULT false"]
   [:immutable :boolean "DEFAULT false"]
   [:editable :boolean "DEFAULT true"]
   [:searchable :boolean "DEFAULT false"]
   [:dependent :boolean "DEFAULT false"]
   [:created_at "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]
   [:updated_at "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]))

(defn migrate
  []
  (create-migration-table)
  (create-model-table)
  (create-field-table))

(migrate)