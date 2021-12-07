(ns {{name}}.http.impl.undertow
  "A tracer-bullet synchronous end-to-end using undertow and pedestal

  There is a lot that could be done to improve this:
  - async pedestal?
  - response mgmt

  Heavily inspired by:
  - https://github.com/ring-clojure/ring/blob/master/ring-jetty-adapter/src/ring/adapter/jetty.clj
  - https://github.com/luminus-framework/ring-undertow-adapter/blob/master/src/ring/adapter/undertow.clj
  - https://github.com/protojure/lib/blob/master/src/protojure/pedestal/core.clj

  Resources:
  - https://undertow.io/javadoc/2.1.x/index.html
    - JavaDoc for undertow
  "
  (:require [clojure.java.io :as io]
            [io.pedestal.http :as http]
            [io.pedestal.interceptor.chain :as pedestal.chain]
            [io.pedestal.http.impl.servlet-interceptor :as servlet-interceptor]
            [io.pedestal.log :as log]
            [ring.adapter.undertow :as ring-adapter
             :refer [tune! http2! client-auth! listen!]]
            [ring.adapter.undertow.request :refer [build-exchange-map]]
            [ring.adapter.undertow.response :refer [set-exchange-response RespondBody]])
  (:import [io.undertow Undertow Undertow$Builder]
           [io.undertow.io AsyncReceiverImpl Receiver$PartialBytesCallback Receiver$FullBytesCallback Receiver$FullStringCallback Receiver$ErrorCallback]
           [io.undertow.server HttpHandler HttpServerExchange]
           [io.undertow.server.handlers BlockingHandler]))

;; Connect the pedestal write-body-to-stream to undertow exchange
(extend-protocol RespondBody
  clojure.lang.Fn
  (respond [f ^HttpServerExchange exchange]
    (servlet-interceptor/write-body-to-stream
     f
     (.getOutputStream exchange))))

;; TODO: I cut some corners to get end-to-end, may want to see what is required to support async?
(defn ^Undertow build-server
  [handler options]
  (log/info :options options)
  (let [^Undertow$Builder builder (Undertow/builder)]
    ;;(handler! handler builder options)
    (.setHandler builder handler)
    (tune! builder options)
    (http2! builder options)
    (client-auth! builder options)
    (listen! builder options)

    (when-some [configurator (:configurator options)]
      (configurator builder))

    (.build builder)))

(defn build-request-map
  "Creates a pedestal request map (out of the ring map that the adapter covers)"
  ([exchange]
   (build-request-map exchange nil))
  ([exchange message]
   ;; VERY IMPORTANT:
   ;; In the async case, build-exchange-map will return an input stream,
   ;; but it was already read!
   (let [request-map  (build-exchange-map exchange)
         _ (log/debug :ring-request-map request-map)
         path-info (.getRequestPath exchange)
         body (if message
                (io/input-stream message)
                (:body request-map))]
     (-> request-map
         (assoc :body body)
         (assoc :path-info path-info)
         (assoc :async-supported? false)))))

(defn process-pedestal-request
  "Uses the service-map and request-map to run pedestal to produce a response-map
  Note this doesn't involve the undertow exchange at all..."
  [service-map request-map]
  (let [;; build a context and execute interceptors
        {::http/keys [interceptors]} service-map
        context-map (pedestal.chain/execute
                     {:request request-map}
                     interceptors)]
    (:response context-map)))

(defn chain-provider-handler-sync
  [service-map]
  (let [websocket? true]
    (BlockingHandler.
     (reify HttpHandler
       (handleRequest [_ exchange]
         (let [request-map  (build-request-map exchange)
               response-map (process-pedestal-request service-map request-map)]
           (log/info :loc :sync-chain-provider :request-map request-map)
           (ring-adapter/handle-request websocket? exchange response-map)))))))

(defn chain-provider-handler
  [service-map]
  (let [websocket? true]
    (reify HttpHandler
      (handleRequest [_ exchange]
        (log/info :test {:in-io? (.isInIoThread exchange)
                         :dispatch-executor (.getDispatchExecutor exchange)})
        (when (.isInIoThread exchange)
          ;; From the undertow docs:
          ;; "IO threads perform non blocking tasks, and should never perform blocking
          ;; operations because they are responsible for multiple connections,
          ;; so while the operation is blocking other connections will essentially hang"
          (.dispatch exchange
                     ^Runnable
                     (fn []
                       (log/info :test {:in-io? (.isInIoThread exchange)})
                       ;; https://undertow.io/undertow-docs/undertow-docs-2.0.0/undertow-request-lifecycle.html
                       ;; We move the exchange from an IO Thread to a worker thread by calling `dispatch`.
                       ;; Once in the worker thread, it is safe to start blocking
                       ;; Blocking allows build-exchange-map to access the undertow input-stream
                       (assert (not (.isInIoThread exchange)) "We must not block on an undertow IO Thread")
                       (.startBlocking exchange)
                       (.receiveFullBytes (.getRequestReceiver exchange)
                                          (reify Receiver$FullBytesCallback
                                            (handle [this exchange message]
                                              ;(log/info :full-bytes-message message)
                                              ;(log/info :string-message (String. message))
                                              (try
                                                (let [request-map (build-request-map exchange message)
                                                      _ (log/debug :request-map request-map)
                                                      response-map (process-pedestal-request service-map request-map)]
                                                  (log/debug :response-map response-map)
                                                  (ring-adapter/handle-request websocket? exchange response-map))
                                                (catch Throwable t
                                                  (log/error :chain-provider-handler-throwable t :exchange exchange)
                                                  (set-exchange-response exchange {:status 500
                                                                                   :body   (.getMessage t)})))))
                                          (reify Receiver$ErrorCallback
                                            (error [this exchange e]
                                              (log/error :undertow-receive-full-bytes-exception e)
                                              (set-exchange-response exchange {:status 500
                                                                               :body (.getMessage e)})))))))))))

;; ==================
;; Pedestal functions
;; ==================

(defn chain-provider-sync
  "A function to use for the `io.pedestal.http/chain-provider` value
  in a service-map, to specify a custom synchronous chain-provider.

  Attaches a `::handler` that knows how to handle synchronous undertow http requests"
  [service-map]
  (assoc service-map ::handler (chain-provider-handler-sync service-map)))

(defn chain-provider
  "A function to use for the `io.pedestal.http/chain-provider` value
  in a service-map, to specify a custom asynchronous chain-provider.

  Attaches a `::handler` that knows how to handle asynchronous undertow http requests"
  [service-map]
  (assoc service-map ::handler (chain-provider-handler service-map)))

(defn server-fn
  "A function to use for the `io.pedestal.http/type` value
  in a service-map, to specify a custom server-fn.

  Uses the provided service map to specify the `::handler` to provide
  the undertow server.

  Compatible with io.pedestal.http create-server and start/stop"
  [service-map {:keys [container-options] :as pedestal-server-options}]
  (let [pedestal-handler (::handler service-map)
        server-options (-> pedestal-server-options
                           (select-keys [:host :port])
                           (merge container-options))
        server (build-server pedestal-handler server-options)]
    {:server server
     :start-fn (fn []
                 (.start server)
                 server)
     :stop-fn (fn []
                (.stop server)
                server)}))
