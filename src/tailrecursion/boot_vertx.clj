(ns tailrecursion.boot-vertx
  {:boot/export-tasks true}
  (:require
    [boot.pod  :as pod]
    [boot.core :as boot]
    [boot.util :as util]))

(def ^:private srv-deps
  '[[org.clojure/data.xml "0.0.8"]
    [io.vertx/vertx-core  "3.3.0"]
    [ring/ring-servlet    "1.6.0-beta1"]])

(defn- warn-deps [deps]
  (let [conflict (delay (util/warn "Overriding Vert.x dependencies, using:\n"))]
    (doseq [dep deps]
      (when (pod/dependency-loaded? dep)
        @conflict
        (util/warn "• %s\n" (pr-str dep))))))

(defn- srv-env [deps]
  (let [dep-syms (->> deps (map first) set)]
    (warn-deps deps)
    (-> (dissoc pod/env :source-paths)
        (update-in [:dependencies] #(remove (comp dep-syms first) %))
        (update-in [:dependencies] into deps))))

(defn- shim [pod]
  (pod/with-eval-in pod
    (def conf (atom nil))
    (defn create [config]
      (reset! conf config)
      (when-let [create-fn (:create @conf)]
        (boot.pod/eval-fn-call [create-fn config])))
    (defn serve [req]
      (if-let [serve-fn (:serve @conf)]
        (boot.pod/eval-fn-call [serve-fn req])
        (throw (Exception. "The required serve function could not be found in web.xml."))))
    (defn destroy []
      (when-let [destroy-fn (:destroy @conf)]
        (boot.pod/eval-fn-call [destroy-fn])))))

(boot/deftask serve
  "Serve the application, refreshing the application with each subsequent invocation."
  [p port PORT int "The port the server will bind to."
   c conf PATH str "The path to the web.xml file"]
  (let [app-dir (boot/tmp-dir!)
        message #(util/info "%s Vert.x on port %s...\n" % port)
        rmpaths #(dissoc % :asset-paths :source-paths :resource-paths :target-path)
        app-env (-> (boot/get-env) (rmpaths) (assoc :resource-paths #{(.getPath app-dir)}))
        app-pod (delay (pod/pod-pool app-env :init shim))
        srv-env (srv-env srv-deps)
        srv-pod (delay (pod/make-pod srv-env))]
    (boot/cleanup
      (message "\nStopping")
      (pod/with-call-in @srv-pod
        (tailrecursion.boot-vertx.impl/terminate!)))
    (boot/with-pre-wrap fileset
      (message (if (realized? srv-pod) "Refreshing" "Starting"))
      (apply boot/sync! app-dir (boot/output-dirs fileset))
      (.invoke @srv-pod
        "tailrecursion.boot-vertx.impl/initiate!" (@app-pod :refresh) port conf)
      fileset)))
