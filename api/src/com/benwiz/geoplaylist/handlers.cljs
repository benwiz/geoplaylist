(ns com.benwiz.geoplaylist.handlers
  (:require [clojure.java.io :as io]
            [com.benwiz.geoplaylist.analyzer :as analyzer]
            [reitit.coercion.malli]
            [ring.util.io :as ring-io]
            [scicloj.ml.dataset :as ds]))

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

(defn- stringify-keywords
  [keywords]
  (into {}
        (map (fn [n]
               [n (if (namespace n)
                    (str (namespace n) "/" (name n))
                    (name n))]))
        keywords))

(defn train
  [{{:strs [spotify-streaming-history-short lastfm-recenttracks google-locations] :as params} :params}]
  (println "Start training...")
  (let [results (analyzer/train {:lastfm-recenttracks-file (:tempfile lastfm-recenttracks)
                                 :google-locations-file    (:tempfile google-locations)})]
    (println "Finish training.")
    {:status  200
     :headers {"Content-Type" "text/csv"}
     :body    (ring-io/piped-input-stream
                #(let [w (io/make-writer % {})]
                   (-> results
                       (ds/rename-columns (stringify-keywords (ds/column-names results)))
                       (ds/write! w {:file-type :csv})
                       .flush)))}))
