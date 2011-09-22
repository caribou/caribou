(ns triface.test.jython
  (:use [clojure.test])
  (:use [triface.debug]))

(import com.instrument.triface.JythonObjectFactory)
(import com.instrument.triface.action.ATrifaceJythonAction)
(import org.python.core.Py)
(import org.python.core.PyString)

(defn set-external-app-sys-path [basepath]
  (let [base "/app"
        dirs ["/controller", "/model", "/view"]]
    (doseq [dir dirs] (.append (.path (Py/getSystemState)) (PyString. (str basepath base dir))))
))
  
(deftest jython-action-test
;; add app path to the python sys path
(set-external-app-sys-path "/Users/feigner/Projects/triface-test-site")
(print (.path (Py/getSystemState)))
;; instantiate a jyton object factory and get a reference to it
(let [jof (JythonObjectFactory. ATrifaceJythonAction "MapMangler")]
  (try		
	  (let [mangler (.createObject jof)]
      (.setMap mangler {:foo "bar", "mp" {"fuck" "yeah!"}})
		  (.execute mangler)
		  (let [m (.getConvertedMap mangler)]
		    (is (= (m :foo) "bar"))
	      (is (= ((m "mp") "fuck" "yeah!"))) 
	      
		    ;; inserted by mapmangler
		    (is (= (m "bar") "baz"))
	      (is (= (m "boolean") true))
	      (is (= (m "int") 1))
	      (is (= (m "long") 1.0))
	      (is (= (m "float") 1.0))
		    (is (= ((m "dict") "key1") "val1"))
	      (is (= ((m "dict") "key2") 2))
	      (is (= (first (m "list")) 1))
	      (is (= (rest (m "list")) [1,2,3,5]))
	    )
  )
  (catch Exception e (print e)))  
))