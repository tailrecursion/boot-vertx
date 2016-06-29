(set-env!
  :resource-paths #{"src" "tst"}
  :source-paths   #{"tst"}
  :dependencies  '[[org.clojure/clojure             "1.8.0"  :scope "provided"]
                   [boot/core                       "2.6.0"  :scope "provided"]
                   [adzerk/bootlaces                "0.1.13" :scope "test"]
                   [adzerk/boot-test                "1.1.1"  :scope "test"]
                   [clj-http                        "3.1.0"  :scope "test"]
                   [ring/ring-mock                  "0.3.0"  :scope "test"]
                   [javax.servlet/javax.servlet-api "3.1.0"]]
 :repositories  [["clojars"       "https://clojars.org/repo/"]
                 ["maven-central" "https://repo1.maven.org/maven2/"]])

(require
  '[adzerk.bootlaces         :refer :all]
  '[adzerk.boot-test         :refer [test]]
  '[tailrecursion.boot-vertx :refer [serve]])

(def +version+ "0.1.0-SNAPSHOT")

(bootlaces! +version+)

(replace-task!
  [t test] (fn [& xs] (comp (web) (serve) (apply t xs))))

(deftask build []
  (comp (test) (build-jar)))

(deftask develop []
  (comp (watch) (speak) (test)))

(deftask demo []
  (comp (web) (watch) (speak) (serve)))

(task-options!
  pom  {:project     'tailrecursion/boot-vertx
        :version     +version+
        :description "Boot VertX server."
        :url         "https://github.com/tailrecursion/boot-vertx"
        :scm         {:url "https://github.com/tailrecursion/boot-vertx"}
        :license     {"EPL" "http://www.eclipse.org/legal/epl-v10.html"}}
  serve {:port       3006}
  test  {:namespaces #{'tailrecursion.boot-vertx-test}}
  web   {:serve      'tailrecursion.boot-vertx-app/serve})
