# geoplaylist

# Get your data

Download your Spotify Extended history here https://www.spotify.com/ca-en/account/privacy/
Download your Google "Location History (Timeline)" here https://takeout.google.com/settings/takeout
If you Scrobble your music into last.fm you can download your play history here instead of using Spotify https://benjaminbenben.com/lastfm-to-csv/

# Spotify APP Credentials (needed for Spotify Web API)

Client id `...`
Client secret `...`

# API

See `api/README.md`

# GUI

- Landing page describing how to "Get your data" from above
- Button to upload location history and music listening history (spotify or last.fm)
  - One request to `POST /upload` that incudes 2 files: (if it is too hard to do multiple files we can split it up)
    1. Google Location History zip file
    2. Music history file, either:
       1. Spotify extended history zip file, or
       2. last.fm csv file
- Take the request's response and use it to create playlists in spotify
- Later
  - Integrate with a map to allow the user's to define location and other things

# TODO

- API
  - finish training properly
- GUI
