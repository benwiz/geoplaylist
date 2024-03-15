# geoplaylist

## Install Clojure

https://clojure.org/guides/install_clojure

## Run

From within the `api/` directory

```
clojure -A:run-m
```

Can use files in `api/resources/` for development. The location data has been randomized.

```
api/resources/google-location-records-scrubbed.json
api/resources/lastfm-recenttracks-20231130.json
```

## Routes

Use these to make sure you can receive a 200 or a 400 from the server

```
/test/health
/test/error
```

Use this to get pre-trained results for faster development

```
/test/csv
```

This is the actual endpoint you'll use to upload files to

```
/train
```
