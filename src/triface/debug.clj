(ns triface.debug)

(defmacro debug [x]
  `(let [x# ~x] (println "debug: " '~x " -> " x#) x#))