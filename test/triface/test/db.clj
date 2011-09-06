(ns triface.test.db
  (:use [triface.core])
  (:use [clojure.test])
  (:use [triface.debug])
  (:require [triface.db :as db])
  (:require [triface.model :as model])
  (:require [clojure.contrib.json :as json]))

(deftest zap-negative-string-test
  (let [zapped (db/zap "foobarbaz")]
    (is (= zapped "foobarbaz"))))

(deftest zap-string-test
  (let [zapped (db/zap "f\\o\"o;b#a%r")]
    (is (= zapped "foobar"))))

(deftest zap-quoted-string-test
  (let [zapped (db/zap "foo'bar")]
    (is (= zapped "foo''bar"))))

(deftest zap-negative-keyword-test
  (let [zapped (db/zap :foobarbaz)]
    (is (= zapped "foobarbaz"))))

(deftest zap-simple-keyword-test
  (let [zapped (db/zap :foo#bar#baz)]
    (is (= zapped "foobarbaz"))))

(deftest zap-int-test
  (let [zapped (db/zap 112358)]
    (is (= zapped 112358))))