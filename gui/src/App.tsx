'use strict';
import React, { useState, useEffect, useMemo } from 'react';
import fetchAsciiArt from './util/asciiFetch';
import SpotifyApi from './util/spotifyApi';
import Loading from './components/Loading/Loading';
import Nav from './components/Nav/Nav';
import Spotify from './components/Spotify/Spotify';
import Upload from './components/Upload/Upload';
import Map from './components/Map/Map';

const spotifyClientId = "641da771c201429da8ec99a659aa5ff6"; // TODO read this from env vars at compile time

// KURT -> ExampleSpotifyUsage is an example, feel free to modify or delete it.
const ExampleSpotifyUsage: React.FC = () => {
  const [spotifyToken, setSpotifyToken] = useState<SpotifyApi.SpotifyToken | null>(() => SpotifyApi.makeToken());
  const [playlistName, setPlaylistName] = useState<string>("");
  const [playlistId, setPlaylistId] = useState<string>("");
  const [uris, setUris] = useState<string>(""); // I'm being lazy since I don't want to create multiple inputs with more complex state, we don't really want a csv here, we want an array of URIs (see SpotifyApi.createPlaylist function signature), but I'm calling `.split(",")` later to get that string[]

  return (
    <>
      {SpotifyApi.isValidToken(spotifyToken) ? (
        <>
          <div style={{display: "block", margin: 5}}>
            <Spotify.LogoutButton callback={() => setSpotifyToken(null)} />
          </div>
          <div style={{display: "block", margin: 5}}>
            <Spotify.LogUserPlaylistsButton token={spotifyToken} />
          </div>
          <div style={{display: "block", margin: 5}}>
            <label>
              {"Playlist Name: "}
              <input type={"text"} onChange={e => setPlaylistName(e.target.value)} /> {/* purposely an uncontrolled component, leaving behind the visible but non-functional playlist name text is is nice ux for this particular example */}
              {" "}
            </label>
            <Spotify.CreatePlaylistButton
              token={spotifyToken}
              name={playlistName}
              callback={(r) => {
                setPlaylistId(r.id);
                setPlaylistName("");
              }}
            />
          </div>
          <div style={{display: "block", margin: 5}}>
            <label>
              {"Playlist ID: "}
              <input type={"text"} value={playlistId} onChange={e => setUris(e.target.value)} />
            </label>
            <label>
              {"Track URI CSV: "}
              <input type={"text"} value={uris} onChange={e => setUris(e.target.value)} />
            </label>
            <Spotify.AddTracksToPlaylistButton
              token={spotifyToken}
              playlistId={playlistId}
              uris={uris.split(",").filter(s => !!s)}
              callback={_ => setUris("")}
            />
          </div>

          <span>Example Track URI CSV (copy and paste this into the input field):</span>
          <span style={{display: "block"}}>spotify:track:4YEU9N2XAE0DfUwxWI5ijA,spotify:track:5GPhq2qHJgSQalqCp0RccS,spotify:track:13NiyfKg0aELrTWvgVL7eH</span>
        </>
      ) : (
        <Spotify.LoginButton clientId={spotifyClientId}/>
      )}
    </>
  );
};

const parseCSV = (csv: string) => {
  const lines = csv.split('\n');
  const header = lines[0].split(',');
  const tracks = [];
  for (const line of lines.slice(1)) {
    const data = line.split(','); // TODO BUG: what if the artist/track/album contains a comma? The line splitting breaks. I will need to eliminate or escape commas in my api.
    if (header.length !== data.length) continue; // this is a temporary hack until I sort out my comma issue
    const track = {};
    for (let i=0; i<data.length; i++) {
      track[header[i]] = data[i];
    }
    tracks.push(track);
  }
  return tracks;
}

const tracks2places = (tracks) => {
  const places = {};
  if (tracks) {
    for (const track of tracks) {
      if (places[track.place]) {
        places[track.place].listens++;
        places[track.place].trackIds.add(track.id);
      } else {
        places[track.place] = {
          listens: 1,
          trackIds: new Set([track.id]),
          // it'd be even better to outer shell or more simply would be the center point and radius, but lat+lng of a random sample is easily good enough for now.
          lat: parseFloat(track.lat),
          lng: parseFloat(track.lng)};
      }
    }
  }
  return places;
};

const App: React.FC = () => {
  const [tracks, setTracks] = useState();
  const places = useMemo(() => tracks2places(tracks), [tracks]);

  useEffect(() => {
    // fetch('http://localhost:8008/test/health')
    //   .then((r) => r.json())
    //   .then(console.log);
    // fetch('http://localhost:8008/test/csv')
    //   .then((r) => r.text())
    //   .then(console.log);
    // fetch('http://localhost:8008/pretrained')
    //   .then((r) => r.text())
    //   .then(console.log);

    fetch('clustered-ds.csv')
      .then((response) => response.text()) // TODO it would be way better to stream the csv directly into a js-object instead of first into a string then into js-object
      .then(parseCSV) // TODO there is a bug in parseCSV
      .then(setTracks)
      .catch((err) => console.error(err));
  }, []);

  return (
    <>
      <ExampleSpotifyUsage />
      {tracks ? (
        <>
          {/* <Nav /> */}
          <Map places={places} />
          {/* <Upload /> */}
        </>
      ) : (
        <Loading />
      )}
    </>
  );
};

export default App;
