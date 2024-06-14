import React, { useState, useEffect } from 'react';
import SpotifyApi from '../../util/spotifyApi';

const LoginButton: React.FC = ({ clientId }: {clientId: string}) => {
  const onClick = (e): void => {SpotifyApi.redirect(clientId)};
  return <button onClick={onClick}>Connect to Spotify</button>;
};

const LogoutButton: React.FN = ({ callback }: {callback: Function}) => {
  const onClick = (e): void => {
    SpotifyApi.clearToken();
    callback();
  };
  return <button onClick={onClick}>Disconnect Spotify</button>;
};

const LogUserPlaylistsButton: React.FN = ({ token }: {token: SpotifyToken}) => {
  const onClick = (e): void => {
    SpotifyApi.getUserPlaylists(token)
      .then((playlists) => {
        console.log(playlists);
      });
  };
  return <button onClick={onClick}>Log Spotify playlists to console</button>;
};

export default { LoginButton, LogoutButton, LogUserPlaylistsButton };
