(ns caribou.app.routing
  (:use 
        [clj-time.core :only (now)]
        [clj-time.format :only (unparse formatters)]
        [compojure.core :only (routes GET POST PUT DELETE ANY)]
        [ring.middleware file file-info]
        [caribou.debug])
  (:require 
            [clojure.string :as string]
            [compojure.handler :as compojure-handler]
            [caribou.app.controller :as controller]
            [caribou.app.halo :as halo]
            [caribou.app.template :as template]
            [caribou.app.util :as app-util]
            [caribou.config :as config]
            [caribou.util :as util]))

;; Routing borrowed heavily (stolen) from Noir

(defonce route-funcs (atom {}))
(defonce caribou-routes (atom {}))

(defn- keyword->symbol [namesp kw]
  (symbol namesp (string/upper-case (name kw))))

(defn- route->key [action rte]
  (let [action (string/replace (str action) #".*/" "")]
    (str action (-> rte
                    (string/replace #"\." "!dot!")
                    (string/replace #"/" "--")
                    (string/replace #":" ">")
                    (string/replace #"\*" "<")))))

(defn- parse-route [[{:keys [fn-name] :as result} [cur :as all]] default-action]
  (let [cur (if (symbol? cur)
              (try
                (deref (resolve cur))
                (catch Exception e
                  (app-util/throwf "Symbol given for route has no value")))
              cur)]
    (when-not (or (vector? cur) (string? cur))
      (app-util/throwf "Routes must either be a string or vector, not a %s" (type cur)))
    (let [[action url] (if (vector? cur)
                         [(keyword->symbol "compojure.core" (first cur)) (second cur)]
                         [default-action cur])
          final (-> result
                    (assoc :fn-name (if fn-name
                                      fn-name
                                      (symbol (route->key action url))))
                    (assoc :url url)
                    (assoc :action action))]
      [final (rest all)])))

(defn- parse-destruct-body [[result [cur :as all]]]
  (when-not (some true? (map #(% cur) [vector? map? symbol?]))
    (app-util/throwf "Invalid destructuring param: %s" cur))
  (-> result
      (assoc :destruct cur)
      (assoc :body (rest all))))

(defn parse-fn-name [[cur :as all]]
  (let [[fn-name remaining] (if (and (symbol? cur)
                                     (or (@route-funcs (keyword (name cur)))
                                         (not (resolve cur))))
                              [cur (rest all)]
                              [nil all])]
    [{:fn-name fn-name} remaining]))

(defn parse-args
  "parses the arguments to defpage. Returns a map containing the keys :name :action :url :destruct :body"
  [args & [default-action]]
  (-> args
      (parse-fn-name)
      (parse-route (or default-action 'compojure.core/GET))
      (parse-destruct-body)))

(defn resolve-method
  [method path func]
  (condp = method
    "GET" (GET path {params :params} func)
    "POST" (POST path {params :params} func)
    "PUT" (PUT path {params :params} func)
    "DELETE" (DELETE path {params :params} func)
    (ANY path {params :params} func)))

(defn add-route
  [method route func]
  (let [[{:keys [action url fn-name]}] (parse-route [{} [route]] 'compojure.core/GET)
        fn-key (keyword fn-name)]
    (log :routing (format "adding route %s %s, name %s" action url fn-name))
    (swap! route-funcs assoc fn-key func)
    (swap! caribou-routes assoc fn-key (resolve-method method url func))))

(defn default-action 
  "if a page doesn't have a defined action, we just send the params"
  [params]
  (assoc params :result (str params)))

(def built-in-formatter (formatters :basic-date-time))

(defn default-index
  [& args]
  (log :routing "Called default-index")
  (format "Welcome to Caribou! Please add some pages, you foolish person.<br /> %s" (unparse built-in-formatter (now))))

; default route
(add-route "GET" "/" default-index)
