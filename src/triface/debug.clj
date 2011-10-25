(ns triface.debug)

(defmacro debug [x]
  `(let [x# ~x] (println "debug:" '~x " -> " x#) x#))

(defmacro log [j x]
  `(let [x# ~x] (println (str ~(name j) ":") x#) x#))

(defmacro local-bindings
  "Produces a map of the names of local bindings to their values."
  []
  (let [symbols (map key @clojure.lang.Compiler/LOCAL_ENV)]
    (zipmap (map (fn [sym] `(quote ~sym)) symbols) symbols)))

(declare ^:dynamic *locals*)
(defn eval-with-locals
  "Evals a form with given locals.  The locals should be a map of symbols to
  values."
  [locals form]
  (binding [*locals* locals]
    (eval
     `(let ~(vec (mapcat #(list % `(*locals* '~%)) (keys locals)))
        ~form))))

(defmacro repl
  "Starts a REPL with the local bindings available."
  []
  `(clojure.main/repl
    :prompt #(print "debug => ")
    :eval (partial eval-with-locals (local-bindings))))

