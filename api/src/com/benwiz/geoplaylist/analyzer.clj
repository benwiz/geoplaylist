(ns com.benwiz.geoplaylist.analyzer
  (:require [cheshire.core :as cheshire]
            [clojure.instant :as inst]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [malli.core :as m]
            [malli.experimental.time :as met]
            [malli.registry :as mr]
            [net.cgrand.xforms :as xf]
            [scicloj.ml.core :as ml]
            [scicloj.ml.dataset :as ds]
            [scicloj.ml.metamorph :as mm])
  (:import (java.time OffsetDateTime ZoneOffset LocalDateTime ZoneId)
           (java.time.format DateTimeFormatter)
           (java.util Date UUID)))

;; Notes
;; - consider streaming the json data into a csv into a dataset, maybe even creating a csv on the filesystem
;; - consider using last.fm instead of spotify, but not everyone scrobbles spotify
;; - it would be interesting to do a blog post on the difference between spotify and last.fm
;; - make it pluggable, just allow using either of them, they should both be easy imports
;; - it would be cool if time was considered in the clustering
;; - :track/timestamp is more relevant than :loc/timestamp... or maybe not. Which do I care about more? Confidence of the song time or confidence of location time?
;; - I wonder if a vector of time parts [YYYY MM DD mm ss nn] would be better

;; TODO probably should use instrumented fns instead of validator and explainer

(mr/set-default-registry! ;; should this be run here or elsewhere?
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

;; (def location-validator (m/validator location-schema))
;; (def location-explainer (m/explainer location-schema))
(def locations-validator (m/validator locations-schema))
(def locations-explainer (m/explainer locations-schema))

(def track-schema
  [:map
   [:id :any]
   [:timestamp :time/offset-date-time]
   ;; the following are all for rendering only, analysis should only use the above for now
   [:artist string?]
   [:album {:optional true} string?]
   [:name string?]])

(def tracks-schema ;; TODO add in sort checking
  [:vector track-schema])

;; (def track-validator (m/validator track-schema))
;; (def track-explainer (m/explainer track-schema))
(def tracks-validator (m/validator tracks-schema))
(def tracks-explainer (m/explainer tracks-schema))

(def results-schema
  [:vector
   [:map
    [:track/id string?]
    [:track/timestamp :time/offset-date-time]
    [:track/artist {:optional true} string?]
    [:track/album {:optional true} string?]
    [:track/name {:optional true} string?]
    [:loc/lat number?]
    [:loc/lng number?]
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

(defn get-spotify-streaming-history-short
  [files]
  (into []
        (mapcat #(cheshire/parse-stream (io/reader %) keyword))
        files))

(defn spotify-tracks-short
  [streams]
  (into []
        (map (fn [{:keys [endTime artistName trackName msPlayed]}]
               {:id        (-> (str "spotify/" artistName "/" trackName) ;; would need to make spotify api request to get the real id, annoying
                               (str/replace \space \_))
                :timestamp (.. (LocalDateTime/parse endTime ;; I asusme this is UTC
                                                    (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm"))
                               (atZone (ZoneId/of "UTC"))
                               toOffsetDateTime)
                :artist    artistName
                ;; :album     "" ;; would need to make spotify api request to know this
                :name      trackName}))
        (reverse streams)))

;; TODO The following 2 functions are just a starting point. I don't yet have the data.

(defn get-spotify-streaming-history-extended
  [files]
  (into []
        (mapcat #(cheshire/parse-stream (io/reader %) keyword))
        files))
(defn spotify-tracks-extended
  [streams]
  (into []
        (map (fn [{:keys [ts spotify_track_uri]}]
               {:id        (-> (str "spotify/" (last (str/split spotify_track_uri ":" 3)))
                               (str/replace \space \_))
                :timestamp (.. (LocalDateTime/parse ts ;; I asusme this is UTC
                                                    (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm"))
                               (atZone (ZoneId/of "UTC"))
                               toOffsetDateTime)
                ;; would need to make spotify api request to know artist, album, name
                ;; :artist    ""
                ;; :album     ""
                ;; :name      ""
                }))
        (reverse streams)))

(comment ;; TODO Explore spotify extended data

  {:ts                "2022-12-01 00:05" ;; endTime
   :ms_played         726600
   :spotify_track_uri "spotify:track:3lqkdtSptgT5JbREVV4U7H"

   :username                          ""
   :platform                          ""
   :conn_country                      ""
   :ip_addr_decrypted                 ""
   :user_agent_decrypted              ""
   :master_metadata_track_name        ""
   :master_metadata_album_artist_name ""
   :master_metadata_album_album_name  ""
   :episode_name                      ""
   :episode_show_name                 ""
   :spotify_episode_uri               ""
   :reason_start                      ""
   :reason_end                        ""
   :shuffle                           ""
   :skipped                           ""
   :offline                           ""
   :offline_timestamp                 ""
   :incognito_mode                    ""}

  )

(defn nearest-location ;; TODO I could probably do this more efficiently from tech.ml.dataset instead of a recursive Clojure fn.
   ;; TODO This is dropping wayyy more tracks than expected, I need to review this whole algorithm
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

(defn parse-inputs ;; TODO it'd be better to validate the real inputs instead of the parsed inputs
  [{:keys [spotify-streaming-history-short-files spotify-streaming-history-extended-files lastfm-recenttracks-file google-locations-file]}]
  (let [locations        (google-locations (get-google-location-records google-locations-file))
        tracks           (cond
                           spotify-streaming-history-short-files
                           (spotify-tracks-short (get-spotify-streaming-history-short spotify-streaming-history-short-files))
                           lastfm-recenttracks-file
                           (lastfm-tracks (get-lastfm-recenttracks lastfm-recenttracks-file)))
        locations-valid? (locations-validator locations)
        tracks-valid?    (tracks-validator tracks)]
    (if-some [error
              (not-empty (cond-> {}
                           (not locations-valid?)
                           (assoc :locations-error (into []
                                                         (map #(update-in % [:value :locations] first))
                                                         (:errors (locations-explainer locations))))
                           (not tracks-valid?)
                           (assoc :tracks-errors (into []
                                                       (map #(update-in % [:value :tracks] first))
                                                       (:errors (tracks-explainer tracks))))))]
      (throw (ex-info "Parsed inputs are invalid." error))
      (do
        (println "Parsed inputs are valid...")
        [locations tracks]))))

(defn train
  [{:keys [spotify-streaming-history-short-files lastfm-recenttracks-file google-locations-file n]
    :or   {n 10}
    :as   args}]
  (assert (or (and (some? spotify-streaming-history-short-files)
                   (nil? lastfm-recenttracks-file))
              (and (nil? spotify-streaming-history-short-files)
                   (some? lastfm-recenttracks-file)))
          "Either spotify-streaming-history-short-files or is allowed lastfm-recenttracks-file")
  (let [[locations tracks] (parse-inputs args)
        tracks-with-loc    (let [locs (nearest-location tracks locations)]
                             (if (results-validator locs)
                               locs
                               (throw (ex-info "Tracks with assigned location is malformed."
                                               {:errors (:errors (results-explainer locs))}))))
        tracks-with-loc    (->> tracks-with-loc
                                ;; TODO Confusingly, filtering here is way less aggressive than filtering before nearest location. I'm confused why it's not exactly the same.
                                ;; Logically, I think it makes more sense to filtering before joining with the location data.
                                (into [] track-filter-xform))
        ds                 (ds/dataset tracks-with-loc {:parser-fn {:loc/timestamp #(.toString %)
                                                                    :track/timestamp #(.toString %)}})
        pipeline-fn        (ml/pipeline (mm/select-columns [:loc/lat :loc/lng])
                                        {:metamorph/id :kmeans-model}
                                        (mm/cluster :k-means [n] :place)) ;; `n` clusters, column name is :place
        trained-ctx        (pipeline-fn {:metamorph/data ds :metamorph/mode :fit})
        clustered-ds       (ds/add-column ds :place (ds/column (:metamorph/data trained-ctx) :place))

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
    clustered-ds))

(comment ;; Scratch

  (take 1 (spotify-tracks-short (get-spotify-streaming-history-short [(io/resource "StreamingHistory0.json") (io/resource "StreamingHistory1.json")])))
  (take 1 (lastfm-tracks (get-lastfm-recenttracks (io/file (io/resource "lastfm-recenttracks-20231130.json")))))
  (take 1 (get-google-location-records (io/file (io/resource "google-location-records.json"))))

  (try
    (def clustered-ds
      (train {
              #_#_:spotify-streaming-history-short-files [(io/file (io/resource "StreamingHistory0.json")) (io/file (io/resource "StreamingHistory1.json"))
                                                      (io/file (io/resource "StreamingHistory2.json")) (io/file (io/resource "StreamingHistory3.json"))
                                                      (io/file (io/resource "StreamingHistory4.json"))]
              :lastfm-recenttracks-file                  (io/file (io/resource "lastfm-recenttracks-20231130.json"))
              :google-locations-file                 (io/file (io/resource "google-location-records.json"))}))
    (catch Exception e
      (or (ex-data e) (throw e))))

  (ds/write! clustered-ds "resources/clustered-ds.csv")


  )
