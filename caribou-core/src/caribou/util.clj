(ns caribou.util
  (:use caribou.debug)
  (:require [clojure.string :as string]
            [clojure.java.io :as io]))

(import java.sql.SQLException)
(import java.io.File)

(defn seq-to-map [f q]
  (reduce #(assoc %1 (f %2) %2) {} q))

(defn slugify [s]
  (.toLowerCase (string/join "_" (re-seq #"[a-zA-Z]+" s))))

(defn titleize [s]
  (string/join " " (map string/capitalize (string/split s #"[^a-zA-Z]+"))))

(def file-separator
  (str (.get (java.lang.System/getProperties) "file.separator")))

(defn pathify
  [paths]
  (string/join file-separator paths))

(defn file-exists?
  [path]
  (.exists (io/file path)))

(defn pull-resource
  [path]
  (io/resource path))

(defn map-vals
  [f m]
  (into {} (for [[k v] m] [k (f v)])))

(defn re-replace
  [r s f]
  (let [between (string/split s r)
        inside (re-seq r s)
        transformed (concat (map f inside) [""])]
    (apply str (interleave between transformed))))

(defn re-replace-first
  [r s f]
  (let [between (string/split s r)
        inside (re-seq r s)
        transformed (concat [(f (first inside))] (rest inside) [""])]
    (apply str (interleave between transformed))))

(defn re-replace-beginning
  [r s]
  (let [[_ after] (re-find r s)]
    after))

(defn render-exception [e]
  (let [cause (.getCause e)]
    (if cause
      (if (isa? cause SQLException)
        (let [next (.getNextException cause)]
          (str next (.printStackTrace next)))
        (str cause (.printStackTrace cause)))
      (str e (.printStackTrace e)))))

(defn get-file-extension [file]
  (let [filename (.getName file)]
  (.toLowerCase (.substring filename (.lastIndexOf filename ".")))))

(defn load-resource
  [resource-name]
  (let [thr (Thread/currentThread)
        ldr (.getContextClassLoader thr)]
    (.getResourceAsStream ldr resource-name)))

(defn load-path [path visit]
  (doseq [file (file-seq (io/file path))]
    (let [filename (.toString file)
          subname (string/replace filename (str path "/") "")]
      (if (.isFile file)
        (visit file subname)))))
