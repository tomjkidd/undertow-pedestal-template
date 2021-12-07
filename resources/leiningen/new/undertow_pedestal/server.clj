(ns {{name}}.server
  "The entrypoint for creating a pedestal server that uses undertow"
  (:require [io.pedestal.http :as server]
            [io.pedestal.http.route :as route]
            [io.pedestal.log :as log]
            [{{name}}.service :as service]
            [{{name}}.http.impl.undertow :as undertow]))

(def service-map
  "The production service-map to provide to pedestal"
  ;; start with production configuration
  (-> service/service
      ;; Wire up interceptors to use
      server/default-interceptors
      (assoc ::server-name "{{name}}-server")))

(def dev-service-map
  "The development service-map to provide pedestal

  - dynamically binds service/routes
  - relaxes security controls
  - adds dev-interceptors"
  (-> service-map
      (merge {:env :dev
              ::server-name "{{name}}-dev-server"
              ;; do not block thread that starts web server
              ::server/join? false
              ;; Routes can be a function that resolve routes,
              ;;  we can use this to set the routes to be reloadable
              ::server/routes #(route/expand-routes (deref #'service/routes))
              ;; all origins are allowed in dev mode
              ::server/allowed-origins {:creds true :allowed-origins (constantly true)}
              ;; Content Security Policy (CSP) is mostly turned off in dev mode
              ::server/secure-headers {:content-security-policy-settings {:object-src "'none'"}}})
      server/dev-interceptors))

(defn run-dev
  "The entry-point for 'lein run-dev'"
  [& _args]
  (println "\nCreating your [DEV] server...")
  (-> dev-service-map
      server/create-server
      server/start))

(defn run-dev-sync
  "The entry-point for 'lein run-dev'"
  [& _args]
  (println "\nCreating your [DEV] server...")
  (-> dev-service-map
      (merge {::server-name "{{name}}-dev-sync-server"
              ::server/chain-provider undertow/chain-provider-sync})
      server/create-server
      server/start))

(declare -main)
(defn restart
  [server]
  (server/stop server)
  (require '{{name}}.service :reload)
  (let [server-name (::server-name server)]
    (cond
      (= server-name "{{name}}-server")
      (do (log/info :msg "Restarting prod server")
          (-main))

      (= server-name "{{name}}-dev-server")
      (do (log/info :msg "Restarting dev async server")
          (run-dev))

      (= server-name "{{name}}-dev-sync-server")
      (do (log/info :msg "Restarting dev sync server")
          (run-dev-sync)))))

(defn stop
  [server]
  (server/stop server))

(defn destroy
  "Tear down server

  Note: This is different than stop, in that a stopped server can
  be restarted. destroy goes further and frees the port by suspending
  the underlying listener. This listener prevents more than one server
  from being run, even after a stop. Destroy allows you to switch between
  the sync and async server in the repl"
  [server]
  (let [s (::server/server server)]
    (mapv (fn [li] (.suspend li))
          (-> server
              ::server/server
              (.getListenerInfo)))
    (server/stop server)))

(defn -main
  "The entry-point for 'lein run'"
  [& _args]
  (println "\nCreating your server...")
  (-> service-map
      server/create-server
      server/start))
