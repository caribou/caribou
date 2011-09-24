(ns triface.core
  (:use compojure.core)
  (:use [clojure.string :only (join split)])
  (:use [clojure.data.json :only (json-str write-json read-json)])
  (:use triface.debug)
  (:require [triface.db :as db]
            [triface.model :as model]
            [triface.util :as util]
            [compojure.route :as route]
            [ring.adapter.jetty :as ring]
            [compojure.handler :as handler]
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
    (let [clauses (split #"," include)
          paths (map #(split #"\." %) clauses)
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
           (let [jsonify (json-str result)
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
     (let ~(vec (apply concat (map (fn [p] [`~p `(~(first path-args) ~(keyword p))]) (rest path-args))))
       (try
         (let [result# ~expr
               format# (~(first path-args) :format)
               handler# (or (format-handlers (keyword format#)) (format-handlers :json))]
           (handler# result# ~(first path-args)))
         (catch Exception e#
           (log :error (str "error rendering /" (join "/" [~@(rest path-args)]) ": "
                     (util/render-exception e#)))
           (json-str
            ~(reduce #(assoc %1 (keyword %2) %2) error path-args)))))))

(defn wrap-response [response meta]
  {:meta (merge {:status "200" :msg "OK"} meta)
   :response response})

;; actions ------------------------------------------------

(action home [params]
  (wrap-response {} {}))

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
          total (db/count slug)
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
  (let [response (render "model" (first (db/query "select * from model where name = '%1'" slug)) {:include {:fields {}}})]
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
  (let [response (render slug (model/create slug (params (keyword slug))) params)]
    (wrap-response response {:type slug})))

(action update-content [params slug id]
  (let [content (model/update slug id (params (keyword slug)))
        response (render slug (db/choose slug id) params)]
    (wrap-response response {:type slug})))

(action delete-content [params slug id]
  (let [content (model/destroy slug id)
        response (render slug content params)]
    (wrap-response response {:type slug})))

;; routes --------------------------------------------------

(defroutes main-routes
  (GET  "/" {params :params} (home params))

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
  (route/resources "/")
  (route/not-found "NONONONONONON"))

(def app (handler/site main-routes))

(defn init []
  (model/init))

(defn start [port]
  (ring/run-jetty (var app) {:port (or port 33333) :join? false}))

(defn go []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "33333"))]
    (init)
    (start port)))

(defn -main []
  (go))