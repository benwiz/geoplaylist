(ns user
  (:require [aleph.http :as http]
            [byte-streams :as bs]
            [manifold.deferred :as md]
            [cheshire.core :as cheshire]
            [clojure.java.io :as io]
            [com.benwiz.geoplaylist :as geoplaylist]))

(def base-url "http://localhost:8008")

(defn restart
  []
  (prn "Starting geoplaylist-server...")
  (geoplaylist/restart geoplaylist/server {:port 8008 :join? false}))

(defn catch-error
  [e]
  (-> e .-data :body bs/to-string
      (cheshire/parse-string keyword)))

(defn get-health-check
  []
  (-> (http/request
        {:method :get
         :url    (str base-url "/test/health")
         :as     :json})
      (md/chain :body)
      (md/catch catch-error)
      deref))

(defn get-error-check
  []
  (-> (http/request
        {:method :get
         :url    (str base-url "/test/error")
         :as     :json})
      (md/catch catch-error)
      deref))

(defn get-csv-check
  []
  (-> (http/request
        {:method  :get
         :url     (str base-url "/test/csv")
         :headers {"Content-Type" "application/csv"}})
      (md/chain (fn [{:keys [body]}]
                  (bs/to-string body)))
      (md/catch catch-error)
      deref))

#_(slurp (io/resource "lastfm-recenttracks-20231130.json"))

(defn post-train
  []
  (-> (http/request
        {:method    :post
         :url       (str base-url "/train")
         :headers   {"Content-Type" "multipart/form-data"
                     "Accept"       "text/csv"}
         :multipart [{:name    "lastfm-recenttracks"
                      :content (io/file (io/resource "lastfm-recenttracks-20231130.json"))}
                     {:name    "google-locations"
                      :content (io/file (io/resource "google-location-records.json"))}]})
      (md/chain (fn [{:keys [body]}]
                  (bs/to-string body)))
      deref))

(comment

  (restart)

  (get-health-check)
  (get-error-check)
  (get-csv-check)

  (post-train)

  )
