(ns caribou.app.handler
  (:use
        [compojure.core :only (routes)]
        caribou.debug)
  (:require
        [compojure.handler :as compojure-handler]
        [caribou.app.util :as app-util]
        [caribou.app.request :as request]
        [caribou.app.routing :as routing]))

(declare reset-handler)

(defn _dynamic-handler
  "calls the dynamic route generation functions and returns a composite handler"
  []
  (-> (apply routes (vals @routing/caribou-routes))
      (request/wrap-request-map)
      (compojure-handler/api)))

(def dynamic-handler (app-util/memoize-visible-atom _dynamic-handler))

(defn reset-handler 
  "clears the memoize atom in the metadata for dynamic-handler, which causes it to 'un-memoize'"
  []
  (app-util/memoize-reset dynamic-handler))
