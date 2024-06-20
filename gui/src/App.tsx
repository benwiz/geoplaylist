'use strict';
import React, { useState, useEffect } from 'react';
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

const App: React.FC = () => {
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [asciiArt, setAsciiArt] = useState<string | null>(null);

  useEffect(() => {
    // fetch('http://localhost:8008/test/health')
    //   .then((r) => r.json())
    //   .then(console.log);
    // fetch('http://localhost:8008/test/csv')
    //   .then((r) => r.text())
    //   .then(console.log);
    // fetch('http://localhost:8008/pretrained') // just an example, a functioning upload is more important than rendering any results right now.
    //   .then((r) => r.text())
    //   .then(console.log);

    fetch('clustered-ds.csv')
      .then((r) => r.text())
      // TODO instead of logging to console, parse the csv into a json object... or maybe not, that's a lot of memory, it's probably better to stream it into just a short array of the "places" column then can use an efficient csv streamer to extract data in real time to build the playlists.
      .then(console.log);

    // Simulate a 2-second loading state when the component first mounts
    const initialLoadingTimeout = setTimeout(() => {
      setIsLoading(false);
    }, 0);

    // Grab ascii art from .txt
    fetchAsciiArt()
      .then(setAsciiArt)
      .catch((e) => {
        console.log('bad ascii request');
        setAsciiArt('no dinos here');
      });

    // Clean up timeout
    return () => clearTimeout(initialLoadingTimeout);
  }, []);

  return (
    <>
      <ExampleSpotifyUsage />

      {isLoading ? (
        <Loading />

      ) : (
        <>
          {/* <Nav /> */}
          <Map />
          {/* <div>
          Welcome, to jurassic park
          {asciiArt && <pre>{asciiArt}</pre>}
          </div>
          <Upload /> */}
        </>
      )}
    </>
  );
};

export default App;
