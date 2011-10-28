(ns triface.test.template.adapter
  (:use [clojure.test])
  (:require [triface.template.adapter :as adapter])
  (import java.io.File))

(def test-properties 
  (into {} (doto (java.util.Properties.)
    (.load (-> (Thread/currentThread)
    (.getContextClassLoader)
    (.getResourceAsStream "test.properties"))))))

(deftest simple-test
   (adapter/init-triface-template-engine (File. (test-properties "baseTemplateDirectory")))
   (println ((adapter/get-renderer (File. "test.ftl")) {"foo" "bar"}))
;;)
)