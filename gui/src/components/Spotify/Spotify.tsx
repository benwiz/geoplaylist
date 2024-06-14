import React, { useState, useEffect } from 'react';
import SpotifyApi from '../../util/spotifyApi';

const LoginButton = ({ clientId }: {clientId: string}) => {
  const onClick = (_e: any): void => {SpotifyApi.redirect(clientId)};
  return <button onClick={onClick}>Connect to Spotify</button>;
};

const LogoutButton = ({ callback }: {callback: Function}) => {
  const onClick = (_e: any): void => {
    SpotifyApi.clearToken();
    callback();
  };
  return <button onClick={onClick}>Disconnect Spotify</button>;
};

const LogUserPlaylistsButton = ({ token }: {token: SpotifyApi.SpotifyToken}) => {
  const onClick = (_e: any): void => {
    SpotifyApi.getUserPlaylists(token)
      .then((playlists: SpotifyApi.Playlist[]) => {
        console.log(playlists);
      });
  };
  return <button onClick={onClick}>Log Spotify playlists to console</button>;
};

const CreatePlaylistButton = ({ token, name }: {token: SpotifyApi.SpotifyToken, name: string}) => {
  const onClick = (_e: any): void => {
    console.log("start create playlist");
    SpotifyApi.createPlaylist(token, name)
      .then((response: SpotifyApi.CreatePlaylistResponse) => {
        console.log(response);
      });
  };
  return <button onClick={onClick} disabled={!name}>Create Playlist</button>;
};

export default { LoginButton, LogoutButton, LogUserPlaylistsButton, CreatePlaylistButton };
