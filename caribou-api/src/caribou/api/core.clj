(ns caribou.api.core
  (:use compojure.core
        [clojure.string :only (join split)]
        [cheshire.core :only (generate-string encode)]
        [cheshire.custom :only (add-encoder)]
        [ring.util.response :only (redirect)]
        [sandbar.auth :only
         (ensure-any-role-if any-role-granted? current-username logout! with-security with-secure-channel)]
        [sandbar.stateful-session :only
         (wrap-stateful-session session-put! session-get session-delete-key!)])
  (:use caribou.debug)
  (:require [caribou.db :as db]
            [caribou.model :as model]
            [caribou.util :as util]
            [caribou.config :as config]
            [caribou.api.account :as account]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.java.jdbc :as sql]
            [clojure.java.io :as io]
            [clojure-csv.core :as csv]
            [clojure.data.xml :as xml]))

(def error
  {:meta {:status "500"
          :msg "Unable to process request"}
   :response {}
   :slug nil})

(defn content-list [slug params]
  (model/rally slug params))

(defn content-item [slug id]
  (db/choose slug id))

(defn content-field [slug id field]
  ((content-item slug id) field))

(defn render [slug content opts]
  (let [model (model/models (keyword slug))]
    (model/model-render model content opts)))

(defn render-field [slug content field opts]
  (model/render (((model/models (keyword slug)) :fields) (keyword field)) content opts))

(defn process-include [include]
  (if (and include (not (empty? include)))
    (let [clauses (split include #",")
          paths (map #(split % #"\.") clauses)
          maps (reduce #(assoc %1 (keyword (first %2)) (process-include (join "." (rest %2)))) {} paths)]
      maps)
    {}))

;; formats -----------------------------------------------

(defn wrap-jsonp [callback result]
  (str callback "(" result ")"))

(defn to-csv-column [bulk key]
  (let [morph (bulk (keyword key))]
    (cond
     (or (seq? morph) (vector? morph) (list? morph)) (join "|" (map #(str (last (first %))) morph))
     :else (str morph))))
                                                   
(defn to-csv [headings bulk]
  (csv/write-csv [(filter identity (map #(to-csv-column bulk %) headings))]))

(def prep-xml)

(defn prep-xml-item [bulk]
  (map (fn [key] [key (prep-xml (bulk key))]) (keys bulk)))
  
(defn prep-xml [bulk]
  (cond
   (map? bulk) (prep-xml-item bulk)
   (or (seq? bulk) (vector? bulk) (list? bulk)) (map (fn [item]
                                                       [:item (prep-xml-item item)])
                                                       bulk)
   :else (str bulk)))

(def format-handlers
  {:json (fn [result params]
           (let [jsonify (generate-string result)
                 jsonp (params :jsonp)]
             (if jsonp
               (wrap-jsonp jsonp jsonify)
               jsonify)))
   :xml  (fn [result params]
           (let [xmlify (prep-xml result)]
             (with-out-str
               (xml/emit (xml/sexp-as-element [:api xmlify])))))
   :csv  (fn [result params]
           (let [bulk (result :response)
                 what (-> result :meta :type)
                 headings (if what (map name (keys ((model/models (keyword what)) :fields))))
                 header (if what (csv/write-csv [headings]) "")]
             (cond
              (map? bulk) (str header (to-csv headings bulk))
              (or (seq? bulk) (vector? bulk) (list? bulk)) (apply str (cons header (map #(to-csv headings %) bulk))))))})

(defmacro action [slug path-args expr]
  `(defn ~slug [~(first path-args)]
     (log :action (str ~(name slug) " => " ~(first path-args)))
     ;; (if (any-role-granted? :admin)
       (let ~(vec (apply concat (map (fn [p] [`~p `(~(first path-args) ~(keyword p))]) (rest path-args))))
         (try
           (let [result# ~expr
                 format# (~(first path-args) :format)
                 handler# (or (format-handlers (keyword format#)) (format-handlers :json))]
             (handler# result# ~(first path-args)))
           (catch Exception e#
             (log :error (str "error rendering /" (join "/" [~@(rest path-args)]) ": "
                              (util/render-exception e#)))
             (generate-string
              ;; (json-str
              ~(reduce #(assoc %1 (keyword %2) %2) error path-args)))))
       ))
       ;; (generate-string
       ;;  (assoc error :meta {:status 403 :msg "you do not have access to this resource"})))))
       ;; (redirect "/permission-denied"))))

(defn wrap-response [response meta]
  {:meta (merge {:status "200" :msg "OK"} meta)
   :response response})

(defn ensure-seq
  "if given a map, convert to a seq containing only its values.
  otherwise, leave it alone"
  [col]
  (try
    (cond
     (map? col) (let [int-test (doall (map #(Integer/parseInt (name %)) (keys col)))]
                  (vals col))
     :else col)
    (catch Exception e
      col)))

(defn ensure-lists-in
  "flatten nested params into lists"
  [params]
  (reduce
   #(if (map? (%1 %2)) (assoc %1 %2 (ensure-seq (%1 %2))) %1)
   params
   (keys params)))

;; actions ------------------------------------------------

(action home [params]
  (wrap-response {} {}))

(defn upload [params]
  (log :action (str "upload => " params))
  (let [upload (params :upload)
        asset (model/create :asset {:filename (upload :filename)
                                    :content_type (upload :content-type)
                                    :size (upload :size)})
        dir (model/asset-dir asset)
        path (model/asset-path asset)
        response (str "
<!doctype html>
<html>
    <head>
    <title>upload response</title>
    </head>
    <body>
        <script type=\"text/javascript\">
            parent.rpc.returnUploadResponse({
                asset_id: " (asset :id) ",
                url: '" path "',
                context: '" (params :context) "',
            });
        </script>
    </body>
</html>
"
                )]
    (.mkdirs (io/file (str "public/" dir)))
    (io/copy (-> params :upload :tempfile) (io/file (str "public/" path)))
    response))

(action list-all [params slug]
  (if (model/models (keyword slug))
    (let [include (process-include (params :include))
          order (or (params :order) "asc")
          order-by (or (params :order_by) "position")
          page_size (or (params :page_size) "30")
          page (Integer/parseInt (or (params :page) "1"))
          limit (Integer/parseInt (or (params :limit) page_size))
          offset (or (params :offset) (* limit (dec page)))
          included (merge params {:include include :limit limit :offset offset :order order :order_by order-by})
          response (map #(render slug % included) (model/rally slug included))
          showing (count response)
          total (db/tally slug)
          extra (if (> (rem total limit) 0) 1 0)
          total_pages (+ extra (quot total limit))]
      (wrap-response response {:type slug
                               :count showing
                               :total_items total
                               :total_pages total_pages
                               :page page
                               :page_size limit
                               :order order
                               :order_by order-by}))
    (merge error {:meta {:msg "no model by that name"}})))

(action model-spec [params slug]
  (let [response (render "model" (first (db/query "select * from model where slug = '%1'" slug)) {:include {:fields {}}})]
    (wrap-response response {:type slug})))

(action item-detail [params slug id]
  (let [include (process-include (params :include))
        response (render slug (content-item slug id) (assoc params :include include))]
    (wrap-response response {:type slug})))

(action field-detail [params slug id field]
  (let [include {(keyword field) (process-include (params :include))}
        response (render-field slug (content-item slug id) field (assoc params :include include))]
    (wrap-response response {})))

(action create-content [params slug]
  (let [response (render slug (model/create slug (ensure-lists-in (params (keyword slug)))) params)]
    (wrap-response response {:type slug})))

(action update-content [params slug id]
  (let [content (model/update slug id (ensure-lists-in (params (keyword slug))))
        response (render slug (db/choose slug id) params)]
    (wrap-response response {:type slug})))

(action delete-content [params slug id]
  (let [content (model/destroy slug id)
        response (render slug content params)]
    (wrap-response response {:type slug})))

(defn permission-denied [params]
  params)

(defn login [params]
  (let [account (first (model/rally :account {:where (str "email = '" (db/zap (params :email)) "'")}))
        crypted (account/crypt (params :password))]
    (if (and account (= crypted (account :crypted_password)))
      (do
        (session-put! :current-account account)
        ;; (str "bobobobobobob" (account :email)))
        (redirect "/"))
      (merge error {:error "login failed"}))))

;; routes --------------------------------------------------

(defroutes main-routes
  (route/files "/" {:root "public"})
  (GET  "/" {params :params} (home params))
  (POST "/upload" {params :params} (upload params))

  (GET  "/permission-denied" {params :params} (permission-denied params))
  (POST "/login" {params :params} (login params))
  (GET  "/:slug.:format" {params :params} (list-all params))
  (POST "/:slug.:format" {params :params} (create-content params))
  (GET  "/:slug/:id.:format" {params :params} (item-detail params))
  (PUT  "/:slug/:id.:format" {params :params} (update-content params))
  (DELETE  "/:slug/:id.:format" {params :params} (delete-content params))
  (GET  "/:slug/:id/:field.:format" {params :params} (field-detail params))

  (GET  "/:slug" {params :params} (list-all params))
  (POST "/:slug" {params :params} (create-content params))
  (GET  "/:slug/:id" {params :params} (item-detail params))
  (PUT  "/:slug/:id" {params :params} (update-content params))
  (DELETE  "/:slug/:id" {params :params} (delete-content params))
  (GET  "/:slug/:id/:field" {params :params} (field-detail params))
  (route/not-found "NONONONONONON"))

(defn authorize
  [request]
  (let [uri (:uri request)
        user (session-get :current-account)]
    (if user
      {:name (user :name) :roles #{:admin}}
      (do
        (session-put! :redirect-uri uri)
        (redirect "/permission-denied")))))

(def security-config
  [#"/login.*" :ssl
   #".*.css|.*.png" :any-channel
   #".*" :nossl])

(def app (-> main-routes
             (with-security authorize)
             handler/site
             wrap-stateful-session
             (db/wrap-db @config/db)
             (with-secure-channel security-config (@config/app :api-port) (@config/app :api-ssl-port))))

(defn init [] )

;; (def header-buffer-size 8388608)

;; (defn start [port ssl-port]
;;   (ring/run-jetty
;;    (var app)
;;    {:port port :join? false
;;     :host "127.0.0.1"
;;     :configurator
;;     (fn [jetty]
;;       (doseq [connector (.getConnectors jetty)]
;;         (.setHeaderBufferSize connector header-buffer-size)))
;;     :ssl? true :ssl-port ssl-port
;;     :keystore "caribou.keystore"
;;     :key-password "caribou"}))

;; (defn go []
;;   (let [port (Integer/parseInt (or (@config/app :api-port) "22222"))
;;         ssl-port (Integer/parseInt (or (@config/app :api-ssl-port) "22223"))]
;;     (start port ssl-port)))

;; (defn -main []
;;   (go))

        ;; ring.middleware.params
        ;; ring.middleware.keyword-params
        ;; ring.middleware.multipart-params
        ;; ring.middleware.session
        ;; ring.middleware.flash
        ;; ring.middleware.cookies)
             ;; wrap-session
             ;; wrap-flash
             ;; wrap-cookies
             ;; wrap-multipart-params
             ;; wrap-params
             ;; ;; wrap-nested-params
             ;; wrap-keyword-params

