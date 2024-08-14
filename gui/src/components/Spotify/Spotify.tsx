import React, { useState, useEffect } from "react";
import SpotifyApi from "../../util/spotifyApi";

const LoginButton = ({ clientId }: { clientId: string }) => {
  const onClick = (_e: any): void => {
    SpotifyApi.redirect(clientId);
  };
  return <button onClick={onClick}>Connect to Spotify</button>;
};

const LogoutButton = ({ callback }: { callback: Function }) => {
  const onClick = (_e: any): void => {
    SpotifyApi.clearToken();
    callback();
  };
  return <button onClick={onClick}>Disconnect Spotify</button>;
};

const LogUserPlaylistsButton = ({ token }: { token: SpotifyApi.SpotifyToken }) => {
  const onClick = (_e: any): void => {
    SpotifyApi.getUserPlaylists(token).then((playlists: SpotifyApi.Playlist[]) => {
      console.log(playlists);
    });
  };
  return <button onClick={onClick}>Log Spotify playlists to console</button>;
};

const CreatePlaylistButton = ({ token, name, callback }: { token: SpotifyApi.SpotifyToken; name: string; callback: Function }) => {
  const [loading, setLoading] = useState(false);
  const onClick = (_e: any): void => {
    setLoading(true);
    SpotifyApi.createPlaylist(token, name)
      .then(callback)
      .then((_) => setLoading(false));
  };
  return (
    <button onClick={onClick} disabled={!name || loading}>
      Create Playlist
    </button>
  );
};

const AddTracksToPlaylistButton = ({ token, playlistId, uris, callback }: { token: SpotifyToken; playlistId: string; uris: string[]; callback: Function }) => {
  const [loading, setLoading] = useState(false);
  const onClick = (_e: any): void => {
    setLoading(true);
    SpotifyApi.addTracksToPlaylist(token, playlistId, uris)
      .then(callback)
      .then((_) => setLoading(false));
  };
  return (
    <button onClick={onClick} disabled={!playlistId || uris.length == 0 || loading}>
      Add to Playlist
    </button>
  );
};

export default {
  LoginButton,
  LogoutButton,
  LogUserPlaylistsButton,
  CreatePlaylistButton,
  AddTracksToPlaylistButton,
};
