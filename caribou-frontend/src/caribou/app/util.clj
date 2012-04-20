(ns caribou.app.util
  (:use
        [caribou.debug]))

(defn throwf [msg & args]
  (throw (Exception. (apply format msg args))))

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

(defn memoize-reset
  [f]
  (reset! (:memoize-atom (meta f)) {}))
