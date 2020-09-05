(defproject lobby "0.1.0-SNAPSHOT"
  :description "Lobby Core Code"
  :min-lein-version "2.7.1"
  :main         lobby.system

  :jar-name     "lobbycore.jar"
  :uberjar-name "lobbycore-standalone.jar"
  
  :repl-options {:init-ns user
                 :timeout 1200000}
  
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520"]
                 [org.clojure/core.async  "0.3.443"]
                 ; Web server
                 [http-kit "2.3.0"]
                 [com.stuartsierra/component "0.3.2"]
                ; routing
                 [compojure "1.6.0"]
                 [ring/ring-defaults "0.3.1"]
                 [clj-http "3.7.0"]
                ; Websocket sente
                 [com.taoensso/sente "1.12.0"]
                ; page rendering
                 [hiccup "1.0.5"]
                 [cljs-http "0.1.46"]
								 [reagent "0.7.0"]
                ; user management
                 [com.cemerick/friend "0.2.3"]
                ; Databasing
                ;[org.clojure/java.jdbc "0.7.5"]
                ;[org.xerial/sqlite-jdbc "3.7.2"]
                ;[org.postgresql/postgresql "9.4-1201-jdbc41"]]
                ]

  :plugins [[lein-figwheel "0.5.14"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]
            [lein-autoexpect "1.9.0"]]

  :source-paths ["src/clj"]

;; https://github.com/emezeske/lein-cljsbuild
;; https://stackoverflow.com/questions/29445260/compile-multiple-cljs-files-to-independent-js-files

  :figwheel { :css-dirs ["resources/public/css"]}  
  
  :cljsbuild {
    :builds {
      :dev {
        :source-paths ["src/cljs"]
        ;; The presence of a :figwheel configuration here
        ;; will cause figwheel to inject the figwheel client
        ;; into your build
        :figwheel true
        :compiler {:main lobby.core
                   ;:externs ["js/externs.js"]
                   :asset-path "js/compiled/out"
                   :output-to "resources/public/js/compiled/lobby.js"
                   :output-dir "resources/public/js/compiled/out"
                   :source-map-timestamp true
                   ;; To console.log CLJS data-structures make sure you enable devtools in Chrome
                   ;; https://github.com/binaryage/cljs-devtools
                   :preloads [devtools.preload]}}
       ;; This next build is a compressed minified build for
       ;; production. You can build this with:
       ;; lein cljsbuild once min
      :min {
        :source-paths ["src/cljs"]
        :compiler {:output-to "resources/public/js/compiled/lobby.js"
                   ;:externs ["js/externs.js"]
                   :main lobby.core
                   :optimizations :advanced
                   :pretty-print false}}}}

  ;; Setting up nREPL for Figwheel and ClojureScript dev
  ;; Please see:
  ;; https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl
  :profiles {
    :uberjar {
      :aot :all
      :source-paths ["src"]
      :prep-tasks   ["compile" ["cljsbuild" "once" "min"]]}
    :dev {
      :dependencies [[reloaded.repl "0.2.4"]
                     [expectations "2.2.0-rc3"]
                     [binaryage/devtools "0.9.4"]
                     [figwheel-sidecar "0.5.14"]
                     [com.cemerick/piggieback "0.2.2"]]
      ;; need to add dev source path here to get user.clj loaded
      :source-paths ["src" "dev"]
      ;; for CIDER
      ;; :plugins [[cider/cider-nrepl "0.12.0"]]
      :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
      ;; need to add the compliled assets to the :clean-targets
      :clean-targets ^{:protect false} ["resources/public/js/compiled" :target-path]}})