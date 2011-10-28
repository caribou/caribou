(ns triface.action.adapter
  (:use triface.debug))
  (import java.io.File)
  (import '(com.instrument.triface.action TrifaceActionEngine ITrifaceAction ITrifaceAction$MapType))

  (defn get-action [script]
    (let [action (TrifaceActionEngine/getAction script)]
      (fn [objectmap]
        (.setMap action objectmap)
        (.execute action)
        (.getConvertedMap action ITrifaceAction$MapType/CLOJURE))))