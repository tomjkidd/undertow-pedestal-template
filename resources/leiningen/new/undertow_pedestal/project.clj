(defproject {{name}} "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [io.pedestal/pedestal.service "0.5.9"]

                 ;; jetty servlet config used for response serialization
                 [io.pedestal/pedestal.jetty "0.5.9"]

                 [ch.qos.logback/logback-classic "1.2.3" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.26"]
                 [org.slf4j/jcl-over-slf4j "1.7.26"]
                 [org.slf4j/log4j-over-slf4j "1.7.26"]

                 [com.carouselapps/to-jdbc-uri "0.5.0"]
                 [org.postgresql/postgresql "42.2.10"]
                 [com.github.seancorfield/next.jdbc "1.2.753"]

                 ;; undertow and prior art
                 [io.undertow/undertow-core "2.2.8.Final"]
                 [io.undertow/undertow-servlet "2.2.8.Final"]
                 [luminus/ring-undertow-adapter "1.2.3"]]
  :exclusions [org.clojure/clojure]
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]
  ;; If you use HTTP/2 or ALPN, use the java-agent to pull in the correct alpn-boot dependency
  ;:java-agents [[org.mortbay.jetty.alpn/jetty-alpn-agent "2.0.5"]]
  :profiles {:dev {:aliases {"run-dev" ["trampoline" "run" "-m" "{{name}}.server/run-dev"]}
                   :dependencies [[io.pedestal/pedestal.service-tools "0.5.9"]]}
             :uberjar {:aot [{{name}}.server]}}
  :main ^{:skip-aot true} {{name}}.server)
