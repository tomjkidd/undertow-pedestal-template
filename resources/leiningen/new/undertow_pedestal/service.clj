(ns {{name}}.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.response :as ring-resp]
            [{{name}}.http.impl.undertow :as undertow]
            [clojure.java.io :as io])
  (:import [clojure.lang Atom]))

(defn about-page
  [_request]
  (ring-resp/response (format "Clojure %s - served from %s"
                              (clojure-version)
                              (route/url-for ::about-page))))

(defn home-page
  [_request]
  (ring-resp/response "Hello World!"))

(defprotocol ListRepo
  (get-list [this]
            "Get the current list")
  (add-item! [this item]
             "Add item to the list")
  (remove-item! [this item]
                "Remove item from the list"))

(extend-protocol ListRepo
  Atom
  (get-list
    [this]
    @this)
  (add-item!
    [this item]
    (swap! this conj item)
    nil)
  (remove-item!
    [this item]
    (swap! this disj item)))

(defn ping-handler
  [request]
  (let [{:keys [json-params database]} request
        {:keys [action parameters]} (:result json-params)
        summary (str "We received action '" action "' with parameters '" parameters "'")]
    (cond
      (= action "item.add")
      (do
        (add-item! database (:item parameters))
        (ring-resp/response {:text summary}))

      (= action "item.remove")
      (do
        (remove-item! database (:item parameters))
        (ring-resp/response {:text summary}))

      :else
      (ring-resp/response {:text (str summary " db: " (get-list database))}))))

(def database (atom #{}))

(def atom-db-interceptor
  {:name :atom-db-interceptor
   :enter
   (fn [context]
     (update context :request assoc :database database))
   :leave
   (fn [context]
     ;; Does nothing
     context)})

(def common-interceptors [(body-params/body-params)
                          http/html-body
                          http/json-body])

(def ping-interceptors (conj common-interceptors atom-db-interceptor))

;; Tabular routes
(def routes #{["/" :get (conj common-interceptors `home-page)]
              ["/about" :get (conj common-interceptors `about-page)]
              ["/ping" :post (conj ping-interceptors `ping-handler) :route-name :ping-post]})

(def service {:env :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; ::http/interceptors []
              ::http/routes #(route/expand-routes routes)

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::http/allowed-origins ["scheme://host:port"]

              ;; Tune the Secure Headers
              ;; and specifically the Content Security Policy appropriate to your service/application
              ;; For more information, see: https://content-security-policy.com/
              ;;   See also: https://github.com/pedestal/pedestal/issues/499
              ;;::http/secure-headers {:content-security-policy-settings {:object-src "'none'"
              ;;                                                          :script-src "'unsafe-inline' 'unsafe-eval' 'strict-dynamic' https: http:"
              ;;                                                          :frame-ancestors "'none'"}}

              ;; Root for resource interceptor that is available by default.
              ::http/resource-path "/public"

              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ;;  This can also be your own chain provider/server-fn -- http://pedestal.io/reference/architecture-overview#_chain_provider
              ;;::http/type :jetty
              ::http/chain-provider undertow/chain-provider
              ::http/type undertow/server-fn

              ;;::http/host "localhost"
              ::http/port 8080
              ;; Options to pass to the container (Jetty)
              ::http/container-options {:h2c? true
                                        :h2? false
                                        ;:keystore "test/hp/keystore.jks"
                                        ;:key-password "password"
                                        :ssl-port 8443
                                        :ssl? true
                                        :keystore (io/resource "undertow-pedestal-keystore.jks")
                                        :key-password "password"
                                        :truststore (io/resource "undertow-pedestal-truststore.jks")
                                        :trust-password "password"
                                        :http2? true
                                        ;; Alternatively, You can specify you're own Jetty HTTPConfiguration
                                        ;; via the `:io.pedestal.http.jetty/http-configuration` container option.
                                        ;:io.pedestal.http.jetty/http-configuration (org.eclipse.jetty.server.HttpConfiguration.)
                                        }})
