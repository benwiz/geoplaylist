'use strict';
import React, { useState, useEffect } from 'react';
import fetchAsciiArt from './util/asciiFetch';
import SpotifyApi from './util/spotifyApi';
import Loading from './components/Loading/Loading';
import Nav from './components/Nav/Nav';
import Spotify from './components/Spotify/Spotify';
import Upload from './components/Upload/Upload';
import Map from './components/Map/Map';

const clientId = "641da771c201429da8ec99a659aa5ff6"; // TODO read this from env vars at compile time

const App: React.FC = () => {
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [asciiArt, setAsciiArt] = useState<string | null>(null);
  const [spotifyToken, setSpotifyToken] = useState<SpotifyApi.SpotifyToken | null>(SpotifyApi.makeToken());
  const [playlistName, setPlaylistName] = useState<string>(null);

  useEffect(() => {

  }, []);

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
      {SpotifyApi.isValidToken(spotifyToken) ? (
        <>
          <div style={{display: "block", margin: 5}}>
            <Spotify.LogoutButton callback={() => setSpotifyToken(null)} />
          </div>
          <div style={{display: "block", margin: 5}}>
            <Spotify.LogUserPlaylistsButton token={spotifyToken} />
          </div>
          <div style={{display: "block", margin: 5}}>
            <label for={"playlistName"} onChange={e => setPlaylistName(e.target.value)}>
              {"Playlist Name: "}
              <input name={"playlistName"} type={"text"} />
            </label>
            <Spotify.CreatePlaylistButton token={spotifyToken} name={playlistName} />
          </div>
        </>
      ) : (
        <Spotify.LoginButton clientId={clientId}/>
      )}

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
