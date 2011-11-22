(ns caribou.test.util
  (:require [caribou.util :as util])
  (:use [clojure.test]))

(import java.io.File)

(deftest get-file-extension-test
  (let [file (File. "/foo/bar/baz/ack/bar/froot.rb")]
    (is (= ".rb" (util/get-file-extension file)))))