(ns triface.action.adapter
  (:use triface.debug)
  (:require [triface.util :as util]))
  (import java.io.File)
  (import '(com.instrument.triface JythonObjectFactory JRubyObjectFactory TrifaceJSObjectFactory))
  (import '(com.instrument.triface.action ITrifaceAction ITrifaceAction$MapType))
  
  (defn resolve-object-factory [script]
    (let [ext (util/get-file-extension script)]
      (case ext
        ".rb" (JRubyObjectFactory. ITrifaceAction script)
        ".py" (JythonObjectFactory. ITrifaceAction script)
        ".js" (TrifaceJSObjectFactory. ITrifaceAction script))))

  (defn get-action [script]
    (let [objectFactory (resolve-object-factory script)
          action (.createObject objectFactory)]
      (fn [objectmap]
        (.setMap action objectmap)
        (.execute action)
        (.getConvertedMap action ITrifaceAction$MapType/CLOJURE))))