(ns caribou.action.adapter
  (:use caribou.debug))
  (import java.io.File)
  (import '(com.instrument.caribou.action CaribouActionEngine ICaribouAction ICaribouAction$MapType))

  (defn get-action [script]
    (let [action (CaribouActionEngine/getAction script)]
      (fn [objectmap]
        (.setMap action objectmap)
        (.execute action)
        (.getConvertedMap action ICaribouAction$MapType/CLOJURE))))