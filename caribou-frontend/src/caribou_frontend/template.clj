(ns caribou.app.template
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [caribou.app.template.freemarker :as freemarker]))

(def templates (ref {}))

(defn load-templates
  "recurse through the view directory and add all the templates that can be found"
  [path]
  (freemarker/init path)
  (loop [fseq (file-seq (io/file path))]
    (if fseq
      (let [filename (.toString (first fseq))
            template-name (string/replace filename (str path "/") "")]
        (if (.isFile (first fseq))
          (let [template (freemarker/render-wrapper template-name)
                template-key (keyword template-name)]
            (dosync
             (alter templates merge {template-key template}))))
        (recur (next fseq))))))
