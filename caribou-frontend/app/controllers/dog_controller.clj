(use 'caribou.app.controller)
(require '[caribou.model :as model])

(controller :home
  :home
  (fn [request]
    (let [dojo (first (model/rally :dojo {:order_by "created_at" :order "desc"}))]
      (merge request {:dojo dojo}))))

(controller :dog
  :dog
  (fn [request]
    (assoc request :dog "DOGDOGDOGDOGDOGDOGDOGDOGDOG-M YOYOYO ")))