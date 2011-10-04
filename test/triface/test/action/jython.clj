;;
;; jython action adapter integration test. 
;;

(ns triface.test.action.jython
  (:use [clojure.test])
  (:require [triface.action.adapter :as adapter]))

(import com.instrument.triface.JythonObjectFactory)
(import java.io.File)

(def test-properties 
  (into {} (doto (java.util.Properties.)
    (.load (-> (Thread/currentThread)
    (.getContextClassLoader)
    (.getResourceAsStream "test.properties"))))))
  
(deftest jython-action-test
(let [action (adapter/get-action (File. (str (test-properties "testApplicationPath") "/MapMangler.py")))]
  (try    
    (let [m (action {:foo "bar", "mp" {"eff" "yes"}})]
        (is (= (m :foo) "bar"))
        (is (= ((m "mp") "eff" "yes"))) 
        
        ;;inserted by mapmangler
        (is (= (m "string") "hello world!"))
        (is (= (m "boolean") true))
        (is (= (m "int") 1))
        (is (= (m "long") 1))
        (is (= (m "float") 1.0))
        (is (= ((m "map") "key1") "val1"))
        (is (= ((m "map") "key2") 2))
        (is (= (first (m "list")) 1))
        (is (= (rest (m "list")) [1,2,3,5]))
      )
  (catch Exception e (print e)))  
))