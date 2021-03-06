(ns tailrecursion.boot-vertx-test
  (:require
    [clj-http.client   :as http]
    [ring.mock.request :as mock]
    [clojure.test   :refer :all]))

(def uri "http://localhost:3006/")

;;; tests ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest test-get
  (let [res (http/request (mock/request :get uri))]
    (is (= 200 (res :status)))
    (is (= (get-in res [:headers :content-type]) "text/plain;charset=iso-8859-1"))
    (is (= "Boot Vert.X" (res :body)))))

(deftest test-post
  (let [res (http/request (update (mock/request :post uri {:foo "bar" :baz "brf"}) :headers dissoc "content-length"))]
    (is (= 200 (res :status)))
    (is (= (get-in res [:headers :content-type]) "text/plain;charset=iso-8859-1"))
    (is (= "Boot Vert.X" (res :body)))))
