(ns tailrecursion.boot-vertx-app)

(defn serve [req]
  {:status  200
   :headers {"content-type" "text/plain;charset=iso-8859-1"}
   :body    "Boot Vert.X"})
