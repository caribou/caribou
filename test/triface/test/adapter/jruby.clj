;;
;; jruby action adapter integration test. 
;;

(ns triface.test.adapter.jruby
  (:use [clojure.test])
  (:use [triface.debug]))

(import com.instrument.triface.JRubyObjectFactory)
(import '(com.instrument.triface.action ITrifaceAction ITrifaceAction$MapType))

(def test-properties 
  (into {} (doto (java.util.Properties.)
    (.load (-> (Thread/currentThread)
    (.getContextClassLoader)
    (.getResourceAsStream "test.properties"))))))
  
(deftest jruby-action-test
;;add app path to the python sys path

;;instantiate a jyton object factory and get a reference to it
(let [jof (JRubyObjectFactory. ITrifaceAction "MapMangler")]
    (.addLoadPath jof (test-properties "testApplicationPath"))
  (try		
	  (let [mangler (.createObject jof)]
      (.setMap mangler {:foo "bar", "mp" {"eff" "yes"}})
		  (.execute mangler)
		  (let [m (.getConvertedMap mangler ITrifaceAction$MapType/CLOJURE)]
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
  )
  (catch Exception e (print e)))  
))