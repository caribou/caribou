(ns caribou.admin.core
  (:use compojure.core)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]))

(defn render-index
  []
  (slurp "public/caribou.html"))

(defroutes admin-routes
  (route/files "/" {:root "public"})
  (route/not-found (render-index)))

(def app (handler/site admin-routes))

(defn init
  [])