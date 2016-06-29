# boot-vertx [![Build Status][1]][2]
a development webserver for boot (WIP)

[](dependency)
```clojure
[tailrecursion/boot-vertx "0.1.0-SNAPSHOT"] ;; latest release
```
[](/dependency)

## overview
THIS TASK IS A WORK IN PROGRESS AND NOT YET RECOMMENDED FOR USE. it is designed to speed up the server-side development workflow by utilizing boot's pods to maintain a clean, interactive environment without the need for annoying jvm restarts or libraries that compromise lisp's ability to clearly express the logic of the problem your application is solving.  it serves an internal distribution from the fileset's output directories (resources and assets).  the distribution mirrors the contents of the environment's target directory, but is served from vertx's classpath as an exploded war file.  

## rationale
at present, this task is a rather naive implementation that fails to make use of vertx's many features and idioms; more of these capabilities will be exposed over time.
* vertx is simple, modular, and non-bloated.
* vertx facilitates multiple languages on the server through [verticles][3].
* vertx has a first class push story that wasn't included as an afterthought.
* vertx is [fast][4]. io is done tthrough netty and java's nio api.

## objectives
* embrace ring to remain backwards compatible, but extend the request and response maps that were based on the old javax servlet api standard with the more convenient nomenclature used by vertx.

## usage
```clojure
(require
  '[adzerk.boot-cljs          :refer [cljs]]
  '[adzerk.boot-reload        :refer [reload]]
  '[hoplon.boot-hoplon        :refer [hoplon]]
  '[tailrecursion.boot-vertx  :refer [serve]])

  (deftask develop []
    (comp (watch) (speak) (web :serve 'myapp/serve) (serve))))
```

## development
to enhance boot-vertx while using a test driven development workflow, from the project root, type:
```bash
boot develop
```

[1]: https://travis-ci.org/tailrecursion/boot-vertx.svg?branch=master
[2]: https://travis-ci.org/tailrecursion/boot-vertx
[3]: http://vertx.io/docs/vertx-core/java/#_verticles
[4]: https://www.techempower.com/benchmarks/#section=data-r8&hw=i7&test=plaintext
