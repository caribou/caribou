(ns caribou.app.routing
  (:use compojure.core
        [clojure.walk :only (stringify-keys)]
        [ring.middleware file file-info]
        [caribou.debug])
  (:require 
            [clojure.java.jdbc :as sql]
            [compojure.handler :as handler]

            [caribou.app.controller :as controller]
            [caribou.app.halo :as halo]
            [caribou.app.template :as template]
            [caribou.config :as config]
            [caribou.db :as db]
            [caribou.model :as model]
            [caribou.util :as util]))

(def pages (ref ()))
(def actions (ref {}))

(defn memoize-visible-atom [f]
  (let [mem (atom {})]
    (with-meta
      (fn [& args]
        (if-let [e (find @mem args)]
          (val e)
          (let [ret (apply f args)]
            (swap! mem assoc args ret)
            ret)))
      {:memoize-atom mem})))

(declare dynamic-handler)

(defn dispatcher
  [request]
  ((dynamic-handler) request))

(defn default-action 
  "if a page doesn't have a defined action, we just send the params"
  [params]
  (assoc params :result (str params)))

(defn retrieve-action
  "Given the controller-key and action-key, return the function that is correspondingly defined by a controller."
  [controller-key action-key]
  (let [controller (@controller/controllers controller-key)
        action (if (empty? controller)
                   default-action
                   (action-key controller))]
    action))

(defn generate-action
  "Depending on the application environment, reload controller files (or not)."
  [page template controller-key action-key]
  (if (= (@config/app :environment) "development")
    (fn [params]
      (do
        (controller/load-controllers "app/controllers")
        (template/load-templates (util/pathify [config/root "app" "templates"]))
        (let [action (retrieve-action controller-key action-key)
              found-template
              (or template
                  (do
                    (template/load-templates (util/pathify [config/root "app" "templates"]))
                    (@template/templates (keyword (page :template)))))]
          (if found-template
            (found-template (stringify-keys (action (assoc params :page page))))
            (str "No template by the name " (page :template))))))
    (let [action (retrieve-action controller-key action-key)]
      (if template
        (fn [params]
          (template (stringify-keys (action (assoc params :page page)))))
        (fn [params] (str "No template by the name " (page :template)))))))


(defn match-action-to-template
  "Make a single route for a single page, given its overarching path (above-path)"
  [page above-path]
  (let [page-path (page :path)
        path (str above-path "/" (if page-path (name page-path) ""))
        controller-key (keyword (page :controller))
        action-key (keyword (page :action))
        method-key (page :method)
        template (@template/templates (keyword (page :template)))
        full (generate-action page template controller-key action-key)]
    (dosync
     (alter actions merge {(keyword (page :action)) full}))
    (concat
     [[path action-key method-key]]
     (mapcat #(match-action-to-template % path) (page :children)))))

(defn make-route
  [[path action method]]
  (cond
   (= method "GET")    (GET path {params :params} ((actions action) params))
   (= method "POST")   (POST path {params :params} ((actions action) params))
   (= method "PUT")    (PUT path {params :params} ((actions action) params))
   (= method "DELETE") (DELETE path {params :params} ((actions action) params))
   :else               (GET path {params :params} ((actions action) params))))

(defn generate-page-routes
  "Given a tree of pages construct and return a list of corresponding routes."
  [pages]
  (let [routes (apply concat (map #(match-action-to-template % "") pages))
        direct (map make-route routes)
        unslashed (filter #(empty? (re-find #"/$" (first %))) routes)
        slashed (map #(cons (str (first %) "/") (rest %)) unslashed)
        indirect (map make-route slashed)]
    (concat direct indirect)))

(defn invoke-pages
  "Call up the pages and arrange them into a tree."
  []
  (let [rows (db/query "select * from page")
        tree (model/arrange-tree rows)]
    (dosync
     (alter pages (fn [a b] b) tree))))

(defn default-index
  [request]
  (str "Welcome to Caribou! Please add some pages, you foolish person."))

(def default-routes (list (GET "/" [] default-index)))

(declare reset-handler)

(defn invoke-routes
  "Invoke pages from the db and generate the routes based on them."
  []
  (log :frontend-routing "loading routes")
  (template/load-templates (util/pathify [config/root "app" "templates"]))
  (sql/with-connection @config/db
    (let [_pages (invoke-pages)
          generated (doall (generate-page-routes @pages))]
      ; we have to pass the reset-handler into halo, this smells
      (concat generated (halo/generate-routes reset-handler) default-routes))))

(defn _dynamic-handler
  "calls the dynamic route generation functions and returns a composite handler"
  []
  (-> (apply routes (invoke-routes))
      (wrap-file (util/pathify [config/root "public"]))
      (wrap-file-info)
      (handler/site)
      (db/wrap-db @config/db)))

(def dynamic-handler (memoize-visible-atom _dynamic-handler))

(defn reset-handler 
  "clears the memoize atom in the metadata for dynamic-handler, which causes it to 'un-memoize'"
  []
  (reset! (:memoize-atom (meta dynamic-handler)) {}))
