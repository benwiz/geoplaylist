(ns com.benwiz.geoplaylist
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.benwiz.geoplaylist.analyzer :as analyzer]
            [com.benwiz.geoplaylist.handlers :as handlers]
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
            [scicloj.ml.dataset :as ds]
            [sieppari.async.manifold] ;; needed for manifold
            [simple-cors.reitit.interceptor :as cors])
  (:import (java.io File)))

(defn routes
  []
  [["/test" {}
    ["/health" {:get handlers/ok}]
    ["/error" {:get handlers/bad}]]
   ["/pretrained" {:get handlers/pretrained}]
   ["/train" {:parameters {:multipart
                           {:file {:filename     string?
                                   :content-type string?
                                   :size         int?
                                   :tempfile     [:fn #(instance? File %)]}}}
              :post       handlers/train}]])

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
     ;; TODO clean up all this commented code.
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
  (let [port 8008]
    (prn (str "Starting geoplaylist-server on port " port
              (when args (str " with args:" args))))
    (restart server (merge {:port port :join? true} args))))

(defn cli
  "Shim to com.benwiz.geoplaylist.analyzer/train wrapping filepaths as file."
  [& {:as args}]
  (println (str "Executing com.benwiz.geoplaylist.analyzer/train with args " (pr-str args)))
  (let [parsed-args
        (into {}
              (map (fn [[k v]]
                     (let [v (str v)]
                       [k
                        (case k
                          :out                   v
                          :google-locations-file (io/resource v)
                          :spotify-streaming-history-extended-files
                          (into []
                                (map io/resource)
                                (str/split v #"\|")))])))
              args)]
    (clojure.pprint/pprint {:parsed-args parsed-args})
    (println "Start training...")
    (-> (analyzer/train parsed-args)
        (ds/write! (:out parsed-args)))
    (println (str "Wrote file to " (:out parsed-args))))
  )
