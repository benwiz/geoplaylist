(ns com.benwiz.geoplaylist.analyzer
  (:require [cheshire.core :as cheshire]
            [clojure.instant :as inst]
            [clojure.java.io :as io]
            [malli.core :as m]
            [malli.experimental.time :as met]
            [malli.registry :as mr]
            [scicloj.ml.core :as ml]
            [scicloj.ml.dataset :as ds]
            [scicloj.ml.metamorph :as mm]
            [net.cgrand.xforms :as xf])
  (:import (java.time OffsetDateTime ZoneOffset LocalDateTime)
           (java.time.format DateTimeFormatter)
           (java.util Date UUID)))

;; Notes
;; - consider streaming the json data into a csv into a dataset, maybe even creating a csv on the filesystem
;; - consider using last.fm instead of spotify, but not everyone scrobbles spotify
;; - it would be interesting to do a blog post on the difference between spotify and last.fm
;; - make it pluggable, just allow using either of them, they should both be easy imports
;; - it would be cool if time was considered in the clustering
;; - :track/timestamp is more relevant than :loc/timestamp
;; - I wonder if a vector of time parts [YYYY MM DD mm ss nn] would be better

(mr/set-default-registry! ;; TODO probably don't run this here
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

(defn get-google-location-records
  [readable]
  (cheshire/parse-stream
    (io/reader readable)
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

(defn get-lastfm-recenttracks
  [readable]
  (cheshire/parse-stream
    (io/reader readable)
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
                 {:id        (str "lastfm/" (or (not-empty mbid) (str (UUID/randomUUID))))
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

(defn nearest-location ;; TODO This is dropping wayyy more tracks than expected, I need to review this whole algorithm
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

(defn track-play-count-filter-xform
  "This filters for tracks that have at least N listens, alternative ideas include most-frequently-listened N tracks"
  [n]
  (comp
    (xf/by-key :track/id (xf/into []))
    (filter (fn [[_id tracks]]
              (> (count tracks) n)))
    (mapcat second)))

(def track-filter-xform (track-play-count-filter-xform 10))

(defn train
  [{:keys [lastfm-recenttracks-file google-locations-file]}]
  (let [ ;; prepare data
        lastfm-recenttracks     (get-lastfm-recenttracks lastfm-recenttracks-file)
        google-location-records (get-google-location-records google-locations-file)
        ;; parse data (TODO later optimization: use scicloj.ml.dataset for all this original pre-processing, it probably can do the join with location data very efficiently.)
        locations               (google-locations google-location-records)
        tracks                  (->> (lastfm-tracks lastfm-recenttracks)
                                     #_(into [] track-filter-xform))
        tracks-with-loc         (->> (nearest-location tracks locations)
                                     ;; TODO Confusingly, filtering here is way less aggressive than filtering before nearest location. I'm confused why it's not exactly the same.
                                     ;; Logically, I think it makes more sense to filtering before joining with the location data.
                                     (into [] track-filter-xform))
        _                       (assert (results-validator tracks-with-loc) "Tracks with assigned location is malformed.")
        ds                      (ds/dataset tracks-with-loc)
        pipeline-fn             (ml/pipeline (mm/select-columns [:loc/lat :loc/lng])
                                             {:metamorph/id :kmeans-model}
                                             (mm/cluster :k-means [504] :location)) ;; I think this means: 504 categories, column name is :location
        trained-ctx             (pipeline-fn {:metamorph/data ds
                                              :metamorph/mode :fit})
        clustered-ds            (ds/add-column ds :location (ds/column (:metamorph/data trained-ctx) :location))

        ;; Proper training and testing that I'll definitely need to do in the future
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

        ]
    ;; TODO I'll end up streaming this clustered-dataset to the http response
    clustered-ds))

(comment ;; Scratch

  (def clustered-ds
    (train {:lastfm-recenttracks-file (io/file (io/resource "lastfm-recenttracks-20231130.json"))
            :google-locations-file    (io/file (io/resource "google-location-records.json"))}))

  (ds/write! clustered-ds "resources/clustered-ds.csv")


  )
