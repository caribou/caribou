(ns caribou.model
  (:use caribou.debug)
  (:use caribou.util)
  (:use [clojure.string :only (join split)])
  (:require [caribou.db :as db]
            [clojure.java.jdbc :as sql]
            [geocoder.core :as geo]
            [triface.app.config :as app-config]))

(import java.text.SimpleDateFormat)
(def simple-date-format (java.text.SimpleDateFormat. "MMMMMMMMM dd', 'yyyy HH':'mm"))
(defn format-date
  "given a date object, return a string representing the canonical format for that date"
  [date]
  (.format simple-date-format date))

(defprotocol Field
  "a protocol for expected behavior of all model fields"
  (table-additions [this field]
    "the set of additions to this db table based on the given name")
  (subfield-names [this field]
    "the names of any additional fields added to the model
    by this field given this name")
  (setup-field [this spec] "further processing on creation of field")
  (cleanup-field [this] "further processing on removal of field")
  (target-for [this] "retrieves the model this field points to, if applicable")
  (update-values [this content values]
    "adds to the map of values that will be committed to the db for this row")
  (post-update [this content]
    "any processing that is required after the content is created/updated")
  (pre-destroy [this content]
    "prepare this content item for destruction")
  (field-from [this content opts]
    "retrieves the value for this field from this content item")
  (render [this content opts] "renders out a single field from this content item"))

(defrecord IdField [row env]
  Field
  (table-additions [this field] [[(keyword field) "SERIAL" "PRIMARY KEY"]])
  (subfield-names [this field] [])
  (setup-field [this spec] nil)
  (cleanup-field [this] nil)
  (target-for [this] nil)
  (update-values [this content values] values)
  (post-update [this content] content)
  (pre-destroy [this content] content)
  (field-from [this content opts] (content (keyword (row :slug))))
  (render [this content opts] (field-from this content opts)))
  
(defrecord IntegerField [row env]
  Field
  (table-additions [this field] [[(keyword field) :integer]])
  (subfield-names [this field] [])
  (setup-field [this spec] nil)
  (cleanup-field [this] nil)
  (target-for [this] nil)

  (update-values [this content values]
    (let [key (keyword (row :slug))]
      (if (contains? content key)
        (try
          (let [value (content key)
                tval (if (isa? (type value) String)
                       (Integer/parseInt value)
                       value)]
            (assoc values key tval))
          (catch Exception e values))
        values)))

  (post-update [this content] content)
  (pre-destroy [this content] content)
  (field-from [this content opts] (content (keyword (row :slug))))
  (render [this content opts] (field-from this content opts)))
  
(defrecord DecimalField [row env]
  Field
  (table-additions [this field] [[(keyword field) :decimal]])
  (subfield-names [this field] [])
  (setup-field [this spec] nil)
  (cleanup-field [this] nil)
  (target-for [this] nil)

  (update-values [this content values]
    (let [key (keyword (row :slug))]
      (if (contains? content key)
        (try
          (let [value (content key)
                tval (if (isa? (type value) String)
                       (BigDecimal. value)
                       value)]
            (assoc values key tval))
          (catch Exception e values))
        values)))

  (post-update [this content] content)
  (pre-destroy [this content] content)
  (field-from [this content opts] (content (keyword (row :slug))))
  (render [this content opts] (str (field-from this content opts))))
  
(defrecord StringField [row env]
  Field
  (table-additions [this field] [[(keyword field) "varchar(256)"]])
  (subfield-names [this field] [])
  (setup-field [this spec] nil)
  (cleanup-field [this] nil)
  (target-for [this] nil)
  (update-values [this content values]
    (let [key (keyword (row :slug))]
      (if (contains? content key)
        (assoc values key (content key))
        values)))

  (post-update [this content] content)
  (pre-destroy [this content] content)
  (field-from [this content opts] (content (keyword (row :slug))))
  (render [this content opts] (field-from this content opts)))

(defrecord SlugField [row env]
  Field
  (table-additions [this field] [[(keyword field) "varchar(256)"]])
  (subfield-names [this field] [])
  (setup-field [this spec] nil)
  (cleanup-field [this] nil)
  (target-for [this] nil)
  (update-values [this content values]
    (let [key (keyword (row :slug))]
      (cond
       (env :link)
         (let [icon (content (keyword (-> env :link :slug)))]
           (if icon
             (assoc values key (slugify icon))
             values))
       (contains? content key) (assoc values key (slugify (content key)))
       :else values)))
  (post-update [this content] content)
  (pre-destroy [this content] content)
  (field-from [this content opts] (content (keyword (row :slug))))
  (render [this content opts] (field-from this content opts)))

(defrecord TextField [row env]
  Field
  (table-additions [this field] [[(keyword field) :text]])
  (subfield-names [this field] [])
  (setup-field [this spec] nil)
  (cleanup-field [this] nil)
  (target-for [this] nil)
  (update-values [this content values]
    (let [key (keyword (row :slug))]
      (if (contains? content key)
        (assoc values key (content key))
        values)))
  (post-update [this content] content)
  (pre-destroy [this content] content)
  (field-from [this content opts] (content (keyword (row :slug))))
  (render [this content opts] (field-from this content opts)))

(defrecord BooleanField [row env]
  Field
  (table-additions [this field] [[(keyword field) :boolean]])
  (subfield-names [this field] [])
  (setup-field [this spec] nil)
  (cleanup-field [this] nil)
  (target-for [this] nil)
  (update-values [this content values]
    (let [key (keyword (row :slug))]
      (if (contains? content key)
        (try
          (let [value (content key)
                tval (if (isa? (type value) String)
                       (Boolean/parseBoolean value)
                       value)]
            (assoc values key tval))
          (catch Exception e values))
        values)))
  (post-update [this content] content)
  (pre-destroy [this content] content)
  (field-from [this content opts] (content (keyword (row :slug))))
  (render [this content opts] (field-from this content opts)))

(defrecord TimestampField [row env]
  Field
  (table-additions [this field] [[(keyword field) "timestamp with time zone" "NOT NULL" "DEFAULT current_timestamp"]])
  (subfield-names [this field] [])
  (setup-field [this spec] nil)
  (cleanup-field [this] nil)
  (target-for [this] nil)
  (update-values [this content values]
    (let [key (keyword (row :slug))]
      (cond
       (= key :updated_at) (assoc values key :current_timestamp)
       (contains? content key) (assoc values key (content key))
       :else values)))
  (post-update [this content] content)
  (pre-destroy [this content] content)
  (field-from [this content opts] (content (keyword (row :slug))))
  (render [this content opts] (format-date (field-from this content opts))))

;; forward reference for Fields that need them
(def make-field)
(def model-render)
(def invoke-model)
(def create)
(def update)
(def destroy)
(def models (ref {}))

(defn pad-break-id [id]
  (let [root (str id)
        len (count root)
        pad-len (- 8 len)
        pad (apply str (repeat pad-len "0"))
        halves (map #(apply str %) (partition 4 (str pad root)))
        path (join "/" halves)]
    path))

(defn asset-dir [asset]
  (str "assets/" (pad-break-id (asset :id))))

(defn asset-path [asset]
  (str (asset-dir asset) "/" (asset :filename)))

(defrecord AssetField [row env]
  Field
  (table-additions [this field] [])
  (subfield-names [this field] [(str field "_id")])
  (setup-field [this spec]
    (update :model (row :model_id)
            {:fields [{:name (titleize (str (row :slug) "_id"))
                       :type "integer"
                       :editable false}]}))
  (cleanup-field [this]
    (let [fields ((models (row :model_id)) :fields)
          id (keyword (str (row :slug) "_id"))]
      (destroy :field (-> fields id :row :id))))
  (target-for [this] nil)
  (update-values [this content values] values)
  (post-update [this content] content)
  (pre-destroy [this content] content)
  (field-from [this content opts]
    (let [asset (or (db/choose :asset (content (keyword (str (row :slug) "_id")))) {})]
      (assoc asset :path (asset-path asset))))
  (render [this content opts] (model-render (models :asset) (field-from this content opts) {})))

(defn full-address [address]
  (join " " [(address :address)
             (address :address_two)
             (address :postal_code)
             (address :city)
             (address :state)
             (address :country)]))

(defn geocode-address [address]
  (let [code (geo/geocode (full-address address))]
    (if (empty? code)
      {}
      {:lat (-> (first code) :location :latitude)
       :lng (-> (first code) :location :longitude)})))

(defrecord AddressField [row env]
  Field
  (table-additions [this field] [])
  (subfield-names [this field] [(str field "_id")])
  (setup-field [this spec]
    (update :model (row :model_id)
            {:fields [{:name (titleize (str (row :slug) "_id"))
                       :type "integer"
                       :editable false}]}))
  (cleanup-field [this]
    (let [fields ((models (row :model_id)) :fields)
          id (keyword (str (row :slug) "_id"))]
      (destroy :field (-> fields id :row :id))))
  (target-for [this] nil)
  (update-values [this content values]
    (let [posted (content (keyword (row :slug)))
          idkey (keyword (str (row :slug) "_id"))
          preexisting (content idkey)
          address (if preexisting (assoc posted :id preexisting) posted)]
      (if address
        ;; (let [location (create :location address)]
        (let [geocode (geocode-address address)
              location (create :location (merge address geocode))]
          (assoc values idkey (location :id)))
        values)))
  (post-update [this content] content)
  (pre-destroy [this content] content)
  (field-from [this content opts]
    (or (db/choose :location (content (keyword (str (row :slug) "_id")))) {}))
  (render [this content opts] (model-render (models :location) (field-from this content opts) {})))

(defn from
  "takes a model and a raw db row and converts it into a full
  content representation as specified by the supplied opts.
  some opts that are supported:
    include - a nested hash of association includes.  if a key matches
    the name of an association any content associated to this item through
    that association will be inserted under that key."
  [model content opts]
  (reduce #(assoc %1 (keyword (-> %2 :row :slug)) (field-from %2 %1 opts)) content (vals (model :fields))))

(defrecord CollectionField [row env]
  Field
  (table-additions [this field] [])
  (subfield-names [this field] [])

  (setup-field [this spec]
    (if (or (nil? (row :link_id)) (zero? (row :link_id)))
      (let [model (models (row :model_id))
            target (models (row :target_id))
            reciprocal-name (or (spec :reciprocal_name) (model :name))
            part (create :field
                   {:name reciprocal-name
                    :type "part"
                    :model_id (row :target_id)
                    :target_id (row :model_id)
                    :link_id (row :id)
                    :dependent (row :dependent)})]
        (db/update :field {:link_id (-> part :row :id)} "id = %1" (row :id)))))

  (cleanup-field [this]
    (try
      (do (destroy :field (-> env :link :id)))
      (catch Exception e (str e))))

  (target-for [this] (models (row :target_id)))

  (update-values [this content values]
    (let [removed (keyword (str "removed_" (row :slug)))]
      (if (content removed)
        (let [ex (map #(Integer/parseInt %) (split (content removed) #","))
              part (env :link)
              part-key (keyword (str (part :slug) "_id"))
              target ((models (row :target_id)) :slug)]
          (if (row :dependent)
            (doall (map #(destroy target %) ex))
            (doall (map #(update target % {part-key nil}) ex)))
          values)
        values)))

  (post-update [this content]
    (let [collection (content (keyword (row :slug)))]
      (if collection
        (let [part (env :link)
              part-key (keyword (str (part :slug) "_id"))
              model (models (part :model_id))
              updated (doall
                       (map
                        #(create
                          (model :slug)
                          (merge % {part-key (content :id)}))
                        collection))]
          (assoc content (keyword (row :slug)) updated))
        content)))

  (pre-destroy [this content]
    (if (or (row :dependent) (-> env :link :dependent))
      (let [parts (field-from this content {:include {(keyword (row :slug)) {}}})
            target (keyword ((target-for this) :slug))]
        (doall (map #(destroy target (% :id)) parts))))
    content)

  (field-from [this content opts]
    (let [include (if (opts :include) ((opts :include) (keyword (row :slug))))]
      (if include
        (let [down (assoc opts :include include)
              link (-> this :env :link :slug)
              parts (db/fetch (-> (target-for this) :slug) (str link "_id = %1 order by %2 asc") (content :id) (str link "_position"))]
          (map #(from (target-for this) % down) parts))
        [])))

  (render [this content opts]
    (map #(model-render (target-for this) % (assoc opts :include ((opts :include) (keyword (row :slug))))) (field-from this content opts))))

(defrecord PartField [row env]
  Field

  (table-additions [this field] [])
  (subfield-names [this field] [(str field "_id") (str field "_position")])

  (setup-field [this spec]
    (let [model_id (row :model_id)
          model (models model_id)
          target (models (row :target_id))
          reciprocal-name (or (spec :reciprocal_name) (model :name))]
      (if (or (nil? (row :link_id)) (zero? (row :link_id)))
        (let [collection (create :field
                           {:name reciprocal-name
                            :type "collection"
                            :model_id (row :target_id)
                            :target_id model_id
                            :link_id (row :id)})]
          (db/update :field {:link_id (-> collection :row :id)} "id = %1" (row :id))))

      (update :model model_id
        {:fields
         [{:name (titleize (str (row :slug) "_id"))
           :type "integer"
           :editable false}
          {:name (titleize (str (row :slug) "_position"))
           :type "integer"
           :editable false}]})))

  (cleanup-field [this]
    (let [fields ((models (row :model_id)) :fields)
          id (keyword (str (row :slug) "_id"))
          position (keyword (str (row :slug) "_position"))]
      (destroy :field (-> fields id :row :id))
      (destroy :field (-> fields position :row :id))
      (try
        (do (destroy :field (-> env :link :id)))
        (catch Exception e (str e)))))

  (target-for [this] (models (-> this :row :target_id)))

  (update-values [this content values] values)

  (post-update [this content] content)

  (pre-destroy [this content] content)

  (field-from [this content opts]
    (let [include (if (opts :include) ((opts :include) (keyword (row :slug))))]
      (if include
        (let [down (assoc opts :include include)
              collector (db/choose (-> (target-for this) :slug) (content (keyword (str (row :slug) "_id"))))]
          (from (target-for this) collector down)))))

  (render [this content opts]
    (let [field (field-from this content opts)]
      (if field
        (model-render (target-for this) field (assoc opts :include ((opts :include) (keyword (row :slug)))))))))

(defrecord TieField [row env]
  Field

  (table-additions [this field] [])
  (subfield-names [this field] [(str field "_id")])

  (setup-field [this spec]
    (let [model_id (row :model_id)
          model (models model_id)]
      (update :model model_id
        {:fields
         [{:name (titleize (str (row :slug) "_id"))
           :type "integer"
           :editable false}]})))

  (cleanup-field [this]
    (let [fields ((models (row :model_id)) :fields)
          id (keyword (str (row :slug) "_id"))]
      (destroy :field (-> fields id :row :id))))

  (target-for [this] this)

  (update-values [this content values] values)

  (post-update [this content] content)

  (pre-destroy [this content] content)

  (field-from [this content opts]
    (let [include (if (opts :include) ((opts :include) (keyword (row :slug))))
          model (models (row :model_id))]
      (if include
        (let [down (assoc opts :include include)
              tie-key (keyword (str (row :slug) "_id"))]
          (if (content tie-key)
            (from model (db/choose (-> model :slug) (content tie-key)) down))))))

  (render [this content opts]
    (let [field (field-from this content opts)]
      (if field
        (model-render (models (row :model_id)) field (assoc opts :include ((opts :include) (keyword (row :slug)))))))))

(defn join-table-name
  "construct a join table name out of two link names"
  [a b]
  (join "_" (sort (map slugify [a b]))))

(defrecord LinkField [row env]
  Field

  (table-additions [this field] [])
  (subfield-names [this field] [])

  (setup-field [this spec]
    (if (or (nil? (row :link_id)) (zero? (row :link_id)))
      (let [model (models (row :model_id))
            target (models (row :target_id))
            reciprocal-name (or (spec :reciprocal_name) (model :name))
            join-name (join-table-name (spec :name) reciprocal-name)
            link (create :field
                   {:name reciprocal-name
                    :type "link"
                    :model_id (row :target_id)
                    :target_id (row :model_id)
                    :link_id (row :id)
                    :dependent (row :dependent)})]
        (create :model
                {:name (join " " (sort (row :name) reciprocal-name))
                 :slug join-name
                 :join_model true
                 :fields
                 [{:name (spec :name)
                   :type "part"
                   :dependent true
                   :reciprocal_name (str reciprocal-name " Join")
                   :target_id (row :target_id)}
                  {:name reciprocal-name
                   :type "part"
                   :dependent true
                   :reciprocal_name (str (spec :name) " Join")
                   :target_id (row :model_id)}]})
        (db/update :field {:link_id (-> link :row :id)} "id = %1" (row :id)))))

  (cleanup-field [this]
    (try
      (do
        (let []
          (destroy :model )
          (destroy :field (-> env :link :id))))
      (catch Exception e (str e))))

  (target-for [this] (models (row :target_id)))

  (update-values [this content values]
    (let [removed (keyword (str "removed_" (row :slug)))]
      (if (content removed)
        (let [ex (map #(Integer/parseInt %) (split (content removed) #","))
              part (env :link)
              part-key (keyword (str (part :slug) "_id"))
              target ((models (row :target_id)) :slug)]
          (if (row :dependent)
            (doall (map #(destroy target %) ex))
            (doall (map #(update target % {part-key nil}) ex)))
          values)
        values)))

  (post-update [this content]
    (let [collection (content (keyword (row :slug)))]
      (if collection
        (let [part (env :link)
              part-key (keyword (str (part :slug) "_id"))
              model (models (part :model_id))
              updated (doall
                       (map
                        #(create
                          (model :slug)
                          (merge % {part-key (content :id)}))
                        collection))]
          (assoc content (keyword (row :slug)) updated))
        content)))

  (pre-destroy [this content]
    (if (or (row :dependent) (-> env :link :dependent))
      (let [parts (field-from this content {:include {(keyword (row :slug)) {}}})
            target (keyword ((target-for this) :slug))]
        (doall (map #(destroy target (% :id)) parts))))
    content)

  (field-from [this content opts]
    (let [include (if (opts :include) ((opts :include) (keyword (row :slug))))]
      (if include
        (let [down (assoc opts :include include)
              link (-> this :env :link :slug)
              parts (db/fetch (-> (target-for this) :slug) (str link "_id = %1 order by %2 asc") (content :id) (str link "_position"))]
          (map #(from (target-for this) % down) parts))
        [])))

  (render [this content opts]
    (map #(model-render (target-for this) % (assoc opts :include ((opts :include) (keyword (row :slug))))) (field-from this content opts))))



  ;; (table-additions [this field] [])
  ;; (subfield-names [this field] [])
  ;; (setup-field [this] nil)
  ;; (cleanup-field [this] nil)
  ;; (target-for [this] nil)
  ;; (update-values [this content values])
  ;; (post-update [this content] content)
  ;; (pre-destroy [this content] content)
  ;; (field-from [this content opts])
  ;; (render [this content opts] ""))

(def field-constructors
  {:id (fn [row] (IdField. row {}))
   :integer (fn [row] (IntegerField. row {}))
   :decimal (fn [row] (DecimalField. row {}))
   :string (fn [row] (StringField. row {}))
   :slug (fn [row] 
           (let [link (db/choose :field (row :link_id))]
             (SlugField. row {:link link})))
   :text (fn [row] (TextField. row {}))
   :boolean (fn [row] (BooleanField. row {}))
   :timestamp (fn [row] (TimestampField. row {}))
   :asset (fn [row] (AssetField. row {}))
   :address (fn [row] (AddressField. row {}))
   :collection (fn [row]
                 (let [link (if (row :link_id) (db/choose :field (row :link_id)))]
                   (CollectionField. row {:link link})))
   :part (fn [row]
           (let [link (db/choose :field (row :link_id))]
             (PartField. row {:link link})))
   :tie (fn [row] (TieField. row {}))
   :link (fn [row] (LinkField. row {}))
   })

(def base-fields [{:name "Id" :type "id" :locked true :immutable true :editable false}
                  {:name "Position" :type "integer" :locked true}
                  {:name "Status" :type "integer" :locked true}
                  {:name "Locale Id" :type "integer" :locked true :editable false}
                  {:name "Env Id" :type "integer" :locked true :editable false}
                  {:name "Locked" :type "boolean" :locked true :immutable true :editable false}
                  {:name "Created At" :type "timestamp" :locked true :immutable true :editable false}
                  {:name "Updated At" :type "timestamp" :locked true :editable false}])

(defn make-field
  "turn a row from the field table into a full fledged Field record"
  [row]
  ((field-constructors (keyword (row :type))) row))

(defn fields-render
  "render all fields out to a string friendly format"
  [fields content opts]
  (reduce #(assoc %1 (keyword (-> %2 :row :slug))
             (render %2 content opts))
          content fields))

(defn model-render
  "render a piece of content according to the fields contained in the model
  and given by the supplied opts"
  [model content opts]
  (fields-render (vals (model :fields)) content opts))

(def lifecycle-hooks (ref {}))

(defn make-lifecycle-hooks
  "establish the set of functions which are called throughout the lifecycle
  of all rows for a given model (slug).  the possible hook points are:
    :before_create     -- called for create only, before the record is made
    :after_create      -- called for create only, now the record has an id
    :before_update     -- called for update only, before any changes are made
    :after_update      -- called for update only, now the changes have been committed
    :before_save       -- called for create and update
    :after_save        -- called for create and update
    :before_destroy    -- only called on destruction, record has not yet been removed
    :after_destroy     -- only called on destruction, now the db has no record of it"
  [slug]
  (let [hooks {(keyword slug)
               {:before_create  (ref {})
                :after_create   (ref {})
                :before_update  (ref {})
                :after_update   (ref {})
                :before_save    (ref {})
                :after_save     (ref {})
                :before_destroy (ref {})
                :after_destroy  (ref {})}}]
    (dosync
     (alter lifecycle-hooks merge hooks))))

(defn run-hook
  "run the hooks for the given model slug given by timing.
  env contains any necessary additional information for the running of the hook"
  [slug timing env]
  (let [kind (lifecycle-hooks (keyword slug))]
    (if kind
      (let [hook (kind (keyword timing))]
        (reduce #((hook %2) %1) env (keys @hook))))))

(defn add-hook
  "add a hook for the given model slug for the given timing.
  each hook must have a unique id, or it overwrites the previous hook at that id."
  [slug timing id hook]
  (dosync
   (alter ((lifecycle-hooks (keyword slug)) (keyword timing))
          merge {id hook})))

(defn invoke-model
  "translates a row from the model table into a nested hash with references
  to its fields in a hash with keys being the field slugs
  and vals being the field invoked as a Field protocol record."
  [model]
  (let [fields (db/query "select * from field where model_id = %1" (model :id))
        field-map (seq-to-map #(keyword (-> % :row :slug)) (map make-field fields))]
    (make-lifecycle-hooks (model :slug))
    (assoc model :fields field-map)))

(defn alter-models
  "inserts a single model into the hash of cached model records."
  [model]
  (dosync
   (alter models merge {(model :slug) model (model :id) model})))

(defn create-model-table
  "create an table with the given name."
  [name]
  (db/create-table (keyword name) []))

(def invoke-models)

(defn add-model-hooks []
  (add-hook :model :before_create :build_table (fn [env]
    (create-model-table (slugify (-> env :spec :name)))
    env))
  
  (add-hook :model :before_create :add_base_fields (fn [env]
    (assoc-in env [:spec :fields] (concat (-> env :spec :fields) base-fields))))

  ;; (add-hook :model :before_save :write_migrations (fn [env]
                                                    
  (add-hook :model :after_create :invoke (fn [env]
    (if (-> env :content :nested)
      (create :field {:name "Parent Id" :model_id (-> env :content :id) :type "integer"}))
    (alter-models (-> env :content))
    env))
  
  (add-hook :model :after_update :rename (fn [env]
    (let [original (-> env :original :slug)
          slug (-> env :content :slug)]
      (if (not (= original slug))
        (db/rename-table original slug)))
    (alter-models (invoke-model (-> env :content)))
    env))

  (add-hook :model :after_save :invoke_all (fn [env]
    (invoke-models)
    env))

  (add-hook :model :after_destroy :cleanup (fn [env]
    (db/drop-table (-> env :content :slug))
    (invoke-models)
    env)))
  
(defn add-field-hooks []
  (add-hook :field :before_save :check_link_slug (fn [env]
    (assoc env :values 
      (if (-> env :spec :link_slug)
        (let [model_id (-> env :spec :model_id)
              link_slug (-> env :spec :link_slug)
              fetch (db/fetch :field "model_id = %1 and slug = '%2'" model_id link_slug)
              linked (first fetch)]
          (assoc (env :values) :link_id (linked :id)))
        (env :values)))))
  
  (add-hook :field :after_create :add_columns (fn [env]
    (let [field (make-field (env :content))
          model_id (-> env :content :model_id)
          model (models model_id)
          slug (if model
                 (model :slug)
                 ((db/choose :model model_id) :slug))
          default (-> env :spec :default_value)]
      (doall (map #(db/add-column slug (name (first %)) (rest %)) (table-additions field (-> env :content :slug))))
      (setup-field field (env :spec))
      (if default
        (db/set-default slug (-> env :content :slug) default))
      (assoc env :content field))))
  
  (add-hook :field :after_update :reify_field (fn [env]
    (let [field (make-field (env :content))
          original (-> env :original :slug)
          slug (-> env :content :slug)
          odefault (-> env :original :default_value)
          default (-> env :content :default_value)
          model (models (-> field :row :model_id))
          spawn (apply zipmap (map #(subfield-names field %) [original slug]))
          transition (apply zipmap (map #(map first (table-additions field %)) [original slug]))]
      (if (not (= original slug))
        (do (doall (map #(update :field (-> ((model :fields) (keyword (first %))) :row :id) {:name (last %)}) spawn))
            (doall (map #(db/rename-column (model :slug) (first %) (last %)) transition))))
      (if (not (= odefault default))
        (db/set-default (model :slug) slug default)))
    (assoc env :content (make-field (env :content)))))

  (add-hook :field :after_destroy :drop_columns (fn [env]
    (let [model (models (-> env :content :model_id))
          field ((model :fields) (keyword (-> env :content :slug)))]
      (do (cleanup-field field))
      (doall (map #(db/drop-column ((models (-> field :row :model_id)) :slug) (first %)) (table-additions field (-> env :content :slug))))
      env))))

(defn invoke-models
  "call to populate the application model cache in model/models.
  (otherwise we hit the db all the time with model and field selects)
  this also means if a model or field is changed in any way that model will
  have to be reinvoked to reflect the current state."
  []
  (let [rows (db/query "select * from model")
        invoked (doall (map invoke-model rows))]
     (add-model-hooks)
     (add-field-hooks)
     (dosync
      (alter models 
        (fn [in-ref new-models] new-models)
        (merge (seq-to-map #(keyword (% :slug)) invoked)
               (seq-to-map #(% :id) invoked))))))

(defn create
  "slug represents the model to be updated.
  the spec contains all information about how to update this row,
  including nested specs which update across associations.
  the only difference between a create and an update is if an id is supplied,
  hence this will automatically forward to update if it finds an id in the spec.
  this means you can use this create method to create or update something,
  using the presence or absence of an id to signal which operation gets triggered."
  [slug spec]
  (if (spec :id)
    (update slug (spec :id) spec)
    (let [model (models (keyword slug))
          values (reduce #(update-values %2 spec %1) {} (vals (dissoc (model :fields) :updated_at)))
          env {:model model :values values :spec spec}
          _save (run-hook slug :before_save env)
          _create (run-hook slug :before_create _save)
          content (db/insert slug (dissoc (_create :values) :updated_at))
          merged (merge (_create :spec) content)
          _after (run-hook slug :after_create (merge _create {:content merged}))
          post (reduce #(post-update %2 %1) (_after :content) (vals (model :fields)))
          _final (run-hook slug :after_save (merge _after {:content post}))]
      (_final :content))))

(defn rally
  "pull a set of content up through the model system with the given options."
  ([slug] (rally slug {}))
  ([slug opts]
     (let [model (models (keyword slug))
           order (or (opts :order) "asc")
           order-by (or (opts :order_by) "position")
           limit (str (or (opts :limit) 30))
           offset (str (or (opts :offset) 0))]
       (doall (map #(from model % opts) (db/query "select * from %1 order by %2 %3 limit %4 offset %5" slug order-by order limit offset))))))

(defn update
  "slug represents the model to be updated.
  id is the specific row to update.
  the spec contains all information about how to update this row,
  including nested specs which update across associations."
  [slug id spec]
  (let [model (models (keyword slug))
        original (db/choose slug id)
        values (reduce #(update-values %2 spec %1) {} (vals (model :fields)))
        env {:model model :values values :spec spec :original original}
        _save (run-hook slug :before_save env)
        _update (run-hook slug :before_update _save)
        success (db/update slug (_update :values) "id = %1" id)
        content (db/choose slug id)
        merged (merge (_update :spec) content)
        _after (run-hook slug :after_update (merge _update {:content merged}))
        post (reduce #(post-update %2 %1) (_after :content) (vals (model :fields)))
        _final (run-hook slug :after_save (merge _after {:content post}))]
    (_final :content)))

(defn destroy
  "destroy the item of the given model with the given id."
  [slug id]
  (let [model (models (keyword slug))
        content (db/choose slug id)
        env {:model model :content content :slug slug}
        _before (run-hook slug :before_destroy env)
        pre (reduce #(pre-destroy %2 %1) (_before :content) (vals (model :fields)))
        deleted (db/delete slug "id = %1" id)
        _after (run-hook slug :after_destroy (merge _before {:content pre}))]
    (_after :content)))

(defn table-columns
  "return a list of all columns for the table corresponding to this model."
  [slug]
  (let [model (models (keyword slug))]
    (apply concat (map (fn [field] (map #(name (first %)) (table-additions field (-> field :row :slug)))) (vals (model :fields))))))

(defn progenitors
  "if the model given by slug is nested,
  return a list of the item given by this id along with all of its ancestors."
  ([slug id] (progenitors slug id {}))
  ([slug id opts]
     (let [model (models (keyword slug))]
       (if (model :nested)
         (let [field-names (table-columns slug)
               base-where (db/clause "id = %1" [id])
               recur-where (db/clause "%1_tree.parent_id = %1.id" [slug])
               before (db/recursive-query slug field-names base-where recur-where)]
           (doall (map #(from model % opts) before)))
         [(from model (db/choose slug id) opts)]))))

(defn descendents
  "pull up all the descendents of the item given by id
  in the nested model given by slug."
  ([slug id] (descendents slug id {}))
  ([slug id opts]
     (let [model (models (keyword slug))]
       (if (model :nested)
         (let [field-names (table-columns slug)
               base-where (db/clause "id = %1" [id])
               recur-where (db/clause "%1_tree.id = %1.parent_id" [slug])
               before (db/recursive-query slug field-names base-where recur-where)]
           (doall (map #(from model % opts) before)))
         [(from model (db/choose slug id) opts)]))))

(defn reconstruct
  "mapping is between parent_ids and collections which share a parent_id.
  node is the item whose descendent tree is to be reconstructed."
  [mapping node]
  (assoc node :children (map #(reconstruct mapping %) (mapping (node :id)))))

(defn arrange-tree
  "given a set of nested items, arrange them into a tree
  based on id/parent_id relationships."
  [items]
  (let [by-parent (group-by #(% :parent_id) items)
        roots (by-parent nil)]
    (doall (map #(reconstruct by-parent %) roots))))

(defn init
  "run any necessary initialization for the model environment."
  []
  ;; (invoke-models)
  (log :model "models-invoked"))

(gen-class
 :name caribou.model.Model
 :prefix model-
 :state state
 :init init
 :constructors {[String] []}
 :methods [[slug [] String]
           [create [clojure.lang.APersistentMap] clojure.lang.APersistentMap]])

(defn model-init [slug]
  [[] slug])

(defn model-create [this spec]
  (sql/with-connection app-config/db
    (create (.state this) spec)))

(defn model-slug [this]
  (.state this))

;; (defmacro 

(try
  (sql/with-connection @app-config/db
    (invoke-models))
  (catch Exception e (str (.toString e) " -- models table does not exist yet")))
