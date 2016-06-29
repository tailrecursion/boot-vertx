(ns tailrecursion.boot-vertx.impl
  (:import
    [io.vertx.core         MultiMap Handler Vertx]
    [io.vertx.core.http    HttpServerRequest HttpServerResponse]
    [io.vertx.core.streams ReadStream WriteStream Pump]
    [java.util             Locale])
  (:require;]
    [boot.pod         :as pod]
    [clojure.data.xml :as xml]
    [clojure.string   :as string]
    [clojure.java.io  :as io]
    [ring.core.protocols :refer [ResponseBody write-body]]))

;;; utilities ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->keyword [s] (keyword (.replace (.toLowerCase s Locale/ENGLISH) \_ \-)))

(defn ->map [^MultiMap nmap]
  (let [lowercase #(.toLowerCase % Locale/ENGLISH)]
    (into {} (map #(vector (lowercase (.getKey %)) (.getValue %)) nmap))))

;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def server (atom nil))

(defn mkcnf [path]
  (let [body #(first (:content (some (fn [$] (if (= (:tag $) %2) $)) %)))]
    (->> (or path "WEB-INF/web.xml")
         (io/resource)
         (io/input-stream)
         (xml/parse)
         (:content)
         (filter #(= (:tag %) :servlet))
         (map :content)
         (some #(if (= (body % :servlet-name) "boot-webapp") %))
         (filter #(= (:tag %) :init-param))
         (map :content)
         (map #(vector (keyword (body % :param-name)) (symbol (body % :param-value))))
         (into {}))))

(defn- get-content-length [^HttpServerRequest req]
  (let [length (.getContentLength req)]
    (if (>= length 0) length)))

(defn protocol [version]
  (case version "HTTP_1_1" "HTTP/1.1"
                "HTTP_1_0" "HTTP/1.0"))

(defn scheme [chain] (if (empty? chain) :http :https))

(defn mkrequest [^HttpServerRequest req]
  {;; local
   :server-host       (-> req .localAddress  .host)         ;; vertx
   :server-port       (-> req .localAddress  .port)         ;; vertx, javax
   :server-name       (-> req .localAddress  .host)         ;; javax: shows localhost instead of host ip address

   ;; remote
   :remote-host       (-> req .remoteAddress .host)         ;; vertx
   :remote-port       (-> req .remoteAddress .port)         ;; vertx
   :remote-addr       (-> req .remoteAddress .host)         ;; javax

   ;; http
   :version           (-> req .version .name)               ;; vertx
   :protocol          (-> req .version .name protocol)      ;; javax
   :method            (-> req .method  .name ->keyword)     ;; vertx
   :request-method    (-> req .method  .name ->keyword)     ;; javax
   :headers           (-> req .headers .entries ->map)      ;; vertx, javax

   ;; uri
   :absolute-uri      (-> req .absoluteURI)                 ;; vertx
   :uri               (-> req .uri)                         ;; vertx, javax
   :scheme            (-> req .peerCertificateChain scheme) ;; javax
   :path              (-> req .path)                        ;; vertx
   :query             (-> req .query)                       ;; vertx
   :query-string      (-> req .query)                       ;; javax

   ;; content
   :ssl-client-cert   (-> req .peerCertificateChain first)  ;; javax
   :body              req})

(defn- set-status [^HttpServerResponse res, status]
  (.setStatus res status))

(defn- set-headers [^HttpServerResponse res, headers]
  (doseq [[key val-or-vals] headers]
    (if (string? val-or-vals)
      (.putHeader res key val-or-vals)
      (doseq [val val-or-vals]
        (.putHeader res key val))))
  (when-let [content-type (get headers "Content-Type")]
    (.setContentType res content-type)))

(extend-protocol ResponseBody
  nil
  (write-body [_ response]
    (.end response))
  String
  (write-body [body response]
    (.end response body))
  clojure.lang.ISeq
  (write-body [body response]
    (doseq [chunk body]
      (.write response (str chunk))))
  java.io.InputStream
  (write-body [body response]
    (with-open [input body]
      (.write response input)))
  java.io.File
  (write-body [body response]
    (with-open [input (io/input-stream body)]
      (.write input response)))
  io.vertx.core.streams.ReadStream
  (write-body [body response]
    (.start (Pump/pump response body))))

(defn set-response! [^HttpServerResponse response {:keys [status headers body] :as res}]
  (when status
    (.setStatusCode response status))
  (doseq [[k v] headers]
    (.putHeader response k v))
  (write-body body response))

;;; handler ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol Lifecycle
  (create  [this])
  (refresh [this pod cnf])
  (destroy [this]))

(deftype PodHandler [pod cnf]
  Handler
  (handle [_ request]
     (let [res (pod/with-pod @pod
                 (serve ~(mkrequest request)))]
       (set-response! (.response request) res)))
  Lifecycle
  (create  [_]
    (pod/with-call-in @pod
      (create ~(deref cnf))))
  (refresh [_ p c]
    (reset! cnf c)
    (reset! pod p))
  (destroy [_]
    (pod/with-call-in @pod
     (destroy))))

;;; impl ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn initiate! [pod port config-path]
  (let [handler #(doto (PodHandler. (atom pod) (atom (mkcnf config-path)))
                       (.create))
        refresh #(doto (.requestHandler @server)
                       (.destroy)
                       (.refresh pod (mkcnf config-path))
                       (.create))
        startup #(doto (.createHttpServer (Vertx/vertx))
                       (.requestHandler (handler))
                       (.listen port))]
    (if @server (refresh) (reset! server (startup)))))

(defn terminate! []
  (doto (.requestHandler @server)
        (.destroy))
  (.close @server))
