(ns user
  (:require [aleph.http :as http]
            [byte-streams :as bs]
            [cheshire.core :as cheshire]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [com.benwiz.geoplaylist :as geoplaylist]
            [com.benwiz.geoplaylist.analyzer :as analyzer]
            [manifold.deferred :as md]))

;; Server management tools

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

(comment ;; Start server

  (restart)

  (get-health-check)
  (get-error-check)
  (get-csv-check)

  (post-train)

  )

;; Scratch

(defn scrub-location-data
  [filename]
  (-> (io/resource filename)
      analyzer/get-google-location-records
      (update :locations
              (fn [locations]
                (into []
                      (map (fn [loc]
                             (assoc loc
                                    :latitudeE7 (+ (rand-int (* 20 10000000)) (* 25 10000000))
                                    :longitudeE7 (- (+ (rand-int (* 60 10000000)) (* 65 10000000))))))
                      locations)))
      (cheshire/generate-stream (io/writer "resources/google-location-records-scrubbed.json"))))

(comment

  (slurp (io/resource "lastfm-recenttracks-20231130.json"))

  (scrub-location-data "google-location-records.json")

  )
