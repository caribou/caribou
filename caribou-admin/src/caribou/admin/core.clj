(ns caribou.admin.core
  (:use compojure.core)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]))

(defn render-index
  [params]
  (slurp "public/caribou.html"))

(defroutes admin-routes
  (route/files "/" {:root "public"})
  (GET "/" {params :params} (render-index params))
  (GET "/:splat" {params :params} (render-index params) :splat #".*"))

(def app
  (handler/site admin-routes))

(defn init
  [])