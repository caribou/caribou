;;
;; jruby action adapter integration test. 
;;

(ns triface.test.action.jruby
  (:use [clojure.test])
  (:require [triface.action.adapter :as adapter]))

(import java.io.File)

(def test-properties 
  (into {} (doto (java.util.Properties.)
    (.load (-> (Thread/currentThread)
    (.getContextClassLoader)
    (.getResourceAsStream "test.properties"))))))

(deftest jruby-action-test
(let [action (adapter/get-action (File. (str (test-properties "testApplicationPath") "/MapMangler.rb")))]
  (try    
    (let [m (action {:foo "bar", "mp" {"eff" "yes"}})]
        (is (= (m :foo) "bar"))
        (is (= ((m "mp") "eff" "yes"))) 
        
        ;; inserted by mapmangler
        (is (= (m "string") "hello world!"))
        (is (= (m "boolean") true))
        (is (= (m "int") 1))
        (is (= (m "float") 1.0))
        (is (= ((m "map") "key1") "val1"))
        (is (= ((m "map") "key2") 2))
        (is (= (first (m "list")) 1))
        (is (= (rest (m "list")) [1,2,3,5]))
      )
  (catch Exception e (print e)))  
))