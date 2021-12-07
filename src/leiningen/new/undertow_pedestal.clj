(ns leiningen.new.undertow-pedestal
  (:require [leiningen.new.templates :as tmpl]
            [leiningen.core.main :as main]))

(def template-name "undertow_pedestal")
(def render (tmpl/renderer template-name))
(def raw (tmpl/raw-resourcer template-name))

(defn undertow-pedestal
  "FIXME: write documentation"
  [name]
  (let [data {:name name
              :sanitized (tmpl/name-to-path name)}]
    (main/info "Generating fresh 'lein new' com.tomjkidd/undertow-pedestal project.")
    (tmpl/->files data
                  ["project.clj" (render "project.clj" data)]
                  ["resources/undertow-pedestal-keystore.jks" (raw "undertow-pedestal-keystore.jks")]
                  ["resources/undertow-pedestal-truststore.jks" (raw "undertow-pedestal-truststore.jks")]
                  ["src/{{sanitized}}/http/impl/undertow.clj" (render "undertow.clj" data)]
                  ["src/{{sanitized}}/service.clj" (render "service.clj" data)]
                  ["src/{{sanitized}}/server.clj" (render "server.clj" data)])))
