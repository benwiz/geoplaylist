(ns com.benwiz.geoplaylist.analyzer
  (:require [cheshire.core :as cheshire]
            [clojure.instant :as inst]
            [clojure.java.io :as io]
            [malli.core :as m]
            [malli.experimental.time :as met]
            [malli.registry :as mr]
            [scicloj.ml.core :as ml]
            [scicloj.ml.dataset :as ds]
            [scicloj.ml.metamorph :as mm])
  (:import (java.time OffsetDateTime ZoneOffset LocalDateTime)
           (java.time.format DateTimeFormatter)
           (java.util Date UUID)))

;; Notes
;; - consider streaming the json data into a csv into a dataset, maybe even creating a csv on the filesystem
;; - consider using last.fm instead of spotify, but not everyone scrobbles spotify
;; - it would be interesting to do a blog post on the difference between spotify and last.fm
;; - make it pluggable, just allow using either of them, they should both be easy imports

(mr/set-default-registry!
  (mr/composite-registry
    (m/default-schemas)
    (met/schemas)))

(def location-schema
  [:map
   [:lat number?]
   [:lng number?]
   [:timestamp :time/offset-date-time]])

(def locations-schema
  [:and
   [:vector location-schema]
   #_[:fn {:error/message "Sort order is wrong."} ;; idk why this isn't working
      (fn [locations]
        (some?
          (reduce
            (fn [prev {:keys [timestamp]}]
              (when prev
                (if (.isBefore timestamp prev)
                  timestamp
                  (reduced nil))))
            (first locations)
            (rest locations))))]])

(def location-validator (m/validator location-schema))
(def location-explainer (m/explainer location-schema))
(def locations-validator (m/validator locations-schema))
(def locations-explainer (m/explainer locations-schema))

(def track-schema
  [:map
   [:id :any]
   ;; TODO I actually want to keep the timezone
   [:timestamp :time/offset-date-time]
   ;; the following are all for rendering only, analysis should only use the above for now
   [:artist string?]
   [:album string?]
   [:name string?]])

(def tracks-schema ;; TODO add in sort checking
  [:vector track-schema])

(def track-validator (m/validator track-schema))
(def track-explainer (m/explainer track-schema))
(def tracks-validator (m/validator tracks-schema))
(def tracks-explainer (m/explainer tracks-schema))

(def results-schema
  [:vector
   [:map
    [:track/id        string?]
    [:track/timestamp :time/offset-date-time]
    [:track/artist    string?]
    [:track/album     string?]
    [:track/name      string?]
    [:loc/lat         number?]
    [:loc/lng         number?]
    [:loc/timestamp :time/offset-date-time]]])

(def results-validator (m/validator results-schema))
(def results-explainer (m/explainer results-schema))



(defonce google-location-records
  (cheshire/parse-stream
    (io/reader (io/resource "google-location-records.json"))
    true))

(defn google-locations
  [records]
  (into []
        (comp
          (filter (fn [{:keys [latitudeE7 longitudeE7 timestamp]}]
                    (and latitudeE7 longitudeE7 timestamp)))
          (map (fn [{:keys [latitudeE7 longitudeE7 timestamp]}]
                 {:lat       (/ latitudeE7 10000000.)
                  :lng       (/ longitudeE7 10000000.)
                  :timestamp (OffsetDateTime/parse timestamp)})))
        (reverse (:locations records))))

(defonce lastfm-recenttracks
  (cheshire/parse-stream
    (io/reader (io/resource "lastfm-recenttracks-20231130.json"))
    (fn [k]
      (case k
        "@attr" :attr
        "#text" :text
        (keyword k)))))

(defn lastfm-tracks
  [pages]
  (into []
        (comp
          (mapcat :track)
          (map (fn [{:keys [artist mbid album name url date]}]
                 {:id        (or (not-empty mbid) (str (UUID/randomUUID)))
                  :timestamp (OffsetDateTime/of
                               (LocalDateTime/ofEpochSecond ;; Jumping through LocalDateTime seems dumb
                                 (Integer/parseInt (:uts date))
                                 0 ZoneOffset/UTC)
                               ZoneOffset/UTC)
                  :artist    (:text artist)
                  :album     (:text album)
                  :name      name})))
        pages))

(comment ;; TODO Explore spotify data

  (def spotify-streams
    (cheshire/parse-stream
      (io/reader (io/resource "StreamingHistory0.json")) ;; this is just the past year's history
      (fn [k]
        (case k
          "@attr" :attr
          "#text" :text
          (keyword k))))
    )

  )

(defn nearest-location
  [initial-tracks initial-locations]
  (loop [track     (first initial-tracks)
         tracks    (rest initial-tracks)
         after     (first initial-locations)
         before    (second initial-locations)
         locations (rest initial-locations) ;; including the `before` location, for ergonomics
         result    []]
    ;; NOTE never walk both locations and tracks simultaneously
    (cond
      ;; no more? stop.
      (or (nil? track) (empty? locations))
      result

      ;; If the track's timestamp is after the after (and by definition the before),
      ;; we need to discard the track, we still haven't yet accessed the overlapping data.
      (.isAfter (:timestamp track) (:timestamp after))
      (recur (first tracks)
             (rest tracks)
             after
             before
             locations
             result)

      ;; If the  track's timestamp is before the before (and by definition before the after),
      ;; we need to discard the location dat, we still haven't yet accessed the overlapping data.
      (.isBefore (:timestamp track) (:timestamp before))
      (recur track
             tracks
             (first locations)
             (second locations)
             (rest locations)
             result)

      ;; If the track is between, update the result and step forward on the tracks only
      (or (.isEqual (:timestamp track) (:timestamp after)) ;; surprisingly they're all almost perfect matches
          (.isEqual (:timestamp track) (:timestamp before))
          (and (.isBefore (:timestamp track) (:timestamp after))
               (.isAfter (:timestamp track) (:timestamp before))))
      (let [after-diff  (- (.toEpochSecond (:timestamp after)) (.toEpochSecond (:timestamp track)))
            before-diff (- (.toEpochSecond (:timestamp track)) (.toEpochSecond (:timestamp before)))]
        (recur (first tracks)
               (rest tracks)
               after
               before
               locations
               (conj result
                     (let [loc (if (< after-diff before-diff) after before)]
                       (-> {}
                           (into
                             (map (fn [[k v]] [(keyword "track" (name k)) v]))
                             track)
                           (into
                             (map (fn [[k v]] [(keyword "loc" (name k)) v]))
                             loc))))))

      :else
      (assert false "Unknown case."))))

(comment ;; Explore the joined data

  (def locations (google-locations google-location-records))
  (def tracks (lastfm-tracks lastfm-recenttracks))

  (locations-validator locations)
  (locations-explainer locations)
  (tracks-validator tracks)

  (def results (nearest-location tracks locations))

  ;;
  ;; Analyze
  ;;

  (count results)
  ;; => 45644

  ;; Surprisingly they're almost all perfect matches on timestamp
  ;; TODO look into this, it's almost too good to be true... unless the step factor
  ;; on the google data is once a second then it makes a lot of sense, especially since
  ;; I'm pretty much always using my phone to play music.
  (frequencies
    (into []
          (map (fn [track]
                 (abs (- (.toEpochSecond (:timestamp track))
                         (.toEpochSecond (:timestamp (:location track)))))))
          results))

  ;; How many different tracks are there? About 1000 with at least 10 plays.
  (->> (into []
             (map :id)
             tracks)
       frequencies
       (into {}
             (filter (fn [[_id c]]
                       (>= c 10))))
       count)

  )

(comment

  (def locations (google-locations google-location-records))
  (def tracks (lastfm-tracks lastfm-recenttracks))
  (def results (nearest-location tracks locations))
  (results-validator results)

  ;; Notes
  ;; - it would be cool if time was considered in the clustering
  ;; - :track/timestamp is more relevant than :loc/timestamp
  ;; - I wonder if a vector of time parts [YYYY MM DD mm ss nn] would be better

  ;; TODO alternatively consider getting my 1000 most played songs, should be easy with ds
  (def frequently-listened ;; TODO do this filtering during the ds/dataset call
    (->> (into []
               (map :id)
               tracks)
         frequencies
         (into #{}
               (keep (fn [[id c]]
                       (when (>= c 10)
                         id))))))

  (def ds
    (ds/dataset
      (into []
            (filter #(contains? frequently-listened (:track/id %)))
            results)))
  (ds/row-count ds)

  (def pipeline-fn
    (ml/pipeline
      (mm/select-columns [:loc/lat :loc/lng])
      {:metamorph/id :kmeans-model}
      (mm/cluster :k-means [504] :location)))

  (def trained-ctx
    (pipeline-fn
      {:metamorph/data ds
       :metamorph/mode :fit}))

  (def clustered-ds
    (ds/add-column ds :location (ds/column (:metamorph/data trained-ctx) :location)))

  ;; Proper training and testing that I'm definitely not doing
  ;; (def split-ds
  ;;   (first
  ;;     (ds/split->seq ds
  ;;                    :holdout
  ;;                    {:ratio       [0.8 0.2]
  ;;                     :split-names [:train-val :test]})))
  ;; (def train-ds (:train-val split-ds))
  ;; (def test-ds (:test split-ds))
  ;; (def train-val-splits (ds/split->seq train-ds :kfold {:k 10}))
  ;; (def evaluations
  ;;   (ml/evaluate-pipelines
  ;;     [pipeline-fn]
  ;;     train-val-splits
  ;;     ml/classification-accuracy
  ;;     :accuracy))
  ;; (def test-ctx
  ;;   (pipe-fn
  ;;     (assoc test-ctx
  ;;            :metamorph/data train-ds
  ;;            :metamorph/mode :fit)))

  (def map-pipeline
    (ml/pipeline
      (mm/select-columns [:loc/lng :loc/lat :location])))
  (ds/write-csv! (:metamorph/data (map-pipeline clustered-ds)) "dev-resources/501.csv")

  ;; View on https://www.arcgis.com/ this is amazing!

  )
