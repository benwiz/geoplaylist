(ns com.benwiz.geoplaylist
  (:require [com.benwiz.geoplaylist.analyzer :as analyzer]
            [muuntaja.core :as m]
            [reitit.coercion.malli]
            [reitit.dev.pretty :as pretty]
            [reitit.http :as http]
            [reitit.http.interceptors.multipart :as multipart]
            [reitit.http.interceptors.muuntaja :as muuntaja]
            [reitit.http.interceptors.parameters :as parameters]
            [reitit.interceptor.sieppari :as s]
            [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty]
            [clojure.java.io :as io]
            [ring.util.io :as ring-io]
            [scicloj.ml.dataset :as ds]
            [sieppari.async.manifold] ;; needed for manifold
            [simple-cors.reitit.interceptor :as cors])
  (:import (java.io File)))

(defn ok
  [_request]
  {:status 200
   :body   {:message "ok"}})

(defn bad
  [_request]
  {:status 400
   :body   {:message "not good"}})

(defn csv
  [_request]
  {:status 200
   :headers {"Content-Type" "text/csv"}
   :body   "a,b,c\n1,2,3\n4,5,6"})

(defn pretrained
  [_request]
  {:status 200
   :body   (slurp (io/resource "clustered-ds.csv"))})

(defn train
  [{{:strs [lastfm-recenttracks google-locations] :as params} :params}]
  (println "Start training...")
  (let [results (analyzer/train {:lastfm-recenttracks-file (:tempfile lastfm-recenttracks)
                                 :google-locations-file    (:tempfile google-locations)})]
    (println "Finish training.")
    {:status  200
     :headers {"Content-Type" "text/csv"}
     :body    (ring-io/piped-input-stream
                #(let [w (io/make-writer % {})]
                   (.flush (ds/write! results w {:file-type :csv}))))}))

(defn routes
  []
  [["/test" {}
    ["/health" {:get ok}]
    ["/error" {:get bad}]
    ["/csv" {:get csv}]]
   ["/pretrained" {:get pretrained}]
   ["/train" {:parameters {:multipart
                           {:file {:filename     string?
                                   :content-type string?
                                   :size         int?
                                   :tempfile     [:fn #(instance? File %)]}}}
              :post       train}]])

(def cors-config
  {:cors-config
   {:allowed-request-methods [:get :post]
    :allowed-request-headers ["Content-Type" "Accept"]
    :allow-credentials?      true
    :origins                 "*"
    :max-age                 300}})

(defn router
  []
  (http/router
    (routes)
    {:exception pretty/exception
     :data      {:interceptors [#_{:leave (fn [ctx]
                                            (clojure.pprint/pprint (:response ctx))
                                            ctx)}
                                (cors/cors-interceptor cors-config)
                                (parameters/parameters-interceptor)
                                ;; {:enter #(update-in % [:request :query-params] walk/keywordize-keys)} ;; parameters-interceptor does not keywordize keys, probably has a good reason for not doing so if spaces are allowed in query-param keys
                                (muuntaja/format-negotiate-interceptor)
                                (muuntaja/format-response-interceptor)
                                ;; (exception/exception-interceptor)
                                (muuntaja/format-request-interceptor)
                                ;; (middleware->interceptor ::cookies cookies/cookies-request cookies/cookies-response)
                                ;; (httpcoercion/coerce-response-interceptor)
                                ;; (httpcoercion/coerce-request-interceptor)
                                (multipart/multipart-interceptor)
                                ;; ring.middleware.multipart-params/wrap-multipart-params
                                ]
                 :muuntaja     m/instance
                 #_#_:coercion (reitit.coercion.malli/create
                                 {:transformers     {:body     {:default reitit.coercion.malli/default-transformer-provider
                                                                :formats {"application/json" reitit.coercion.malli/json-transformer-provider}}
                                                     :string   {:default reitit.coercion.malli/string-transformer-provider}
                                                     :response {:default reitit.coercion.malli/default-transformer-provider}}
                                  :error-keys       #{:type :coercion :in :schema :value :errors :humanized #_:transformed}
                                  :lite             true
                                  :compile          (fn ([sch] sch) ([sch opts] sch))
                                  :validate         true
                                  :enabled          true
                                  :strip-extra-keys true
                                  :default-values   true
                                  :options          nil})}
     :reitit.http/default-options-endpoint
     (cors/make-default-options-endpoint cors-config)}))

(defn handler
  []
  (http/ring-handler
    (router)
    (ring/routes
      (ring/create-resource-handler)
      (ring/create-default-handler))
    {:executor s/executor}))

(def app (handler))

(defn restart
  ([server]
   (restart server nil))
  ([server {:keys [port join?]}]
   (swap! server
          (fn [old-server]
            (when old-server
              (prn "stopping old server")
              (.stop old-server))
            (let [server (jetty/run-jetty
                           #'app
                           {:port   port
                            :join?  join? ;; will block thread if true
                            :async? false})]
              (prn "server running on http port " port)
              server)))))

(defonce server (atom nil))

(defn -main
  [& {:as args}]
  (prn "Starting geoplaylist-server..." args)
  (restart server (merge {:port 8008 :join? true} args)))
