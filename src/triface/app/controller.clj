(ns triface.app.controller)

(defn reset-action []
  (def action (fn [params] (merge params {:result (str "somehow you are executing the reset action" params)}))))

(reset-action)