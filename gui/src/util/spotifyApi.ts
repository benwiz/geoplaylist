'use strict';

type SpotifyToken = {
  accessToken: string;
  expiresAt: Date;
};

const removeHash = (): void => {
  history.pushState("", document.title, window.location.pathname + window.location.search);
};

/** Create token if available from the url then redirect to remove the hash.
If not in url, try to get the token from localStorage.
Return null if no token available. **/
const makeToken: SpotifyToken | null | void = () => {
  if (window.location.hash) {
    const hash = new URLSearchParams(window.location.hash.substring(1));
    const expiresIn = parseInt(hash.get("expires_in"));
    const token = {
      accessToken: hash.get("access_token"),
      expiresAt: new Date().getTime() + (expiresIn * 1000)
    };
    window.localStorage.setItem("spotify-token", JSON.stringify(token));
    // const path = hash.get("state"); // TODO instead of removing hash I should replace the hash with this
    removeHash();
  } else {
    const storedToken: string = window.localStorage.getItem("spotify-token");
    const token: SpotifyToken | null = JSON.parse(storedToken);
    if (token) {
      token.expiresAt = new Date(token.expiresAt);
      return token;
    } else {
      return null;
    }
  }
};

const clearToken = (): void => {
  window.localStorage.removeItem("spotify-token");
};

/** Return true or false depending if it is still valid.
If it is invalid, remove the localStorage entry. **/
const isValidToken = (token: SpotifyToken): boolean => {
  if (token) {
    if (new Date < token.expiresAt) {
      return true;
    } else {
      clearToken();
      return false;
    }
  }
};

const redirect = (clientId: string): void => {
  const scopes = encodeURIComponent([
    "playlist-read-private",
    "playlist-modify-private",
    "playlist-modify-public"].join(" "));
  const redirectUri = window.location.origin;
  const path = window.location.pathname;
  const hash = window.location.hash;
  const spotifyLoginUri = [
    "https://accounts.spotify.com/authorize",
    "?response_type=", "token", // TODO optimization: use `code` but need backend to continue the authorization, allows for refreshing
    "&client_id=", clientId,
    "&scope=", scopes,
    "&redirect_uri=", encodeURIComponent(redirectUri + path),
    "&state=", encodeURIComponent(hash)].join(""); // maintain the hash state for future use... we're not using hash currently so it doesn't matter.
  window.location = spotifyLoginUri;
};

type Playlist = {
  collaborative: boolean;
  description: string;
  external_urls: { spotify: string };
  href: string;
  id: string;
  images: { height: number | null, url: string, width: number | null }[];
  name: string;
  owner: {
    display_name: string;
    external_urls: { spotify: string };
    href: string;
    id: string;
    type: string;
    uri: string;
  };
  primary_color: string | null;
  public: boolean;
  snapshot_id: string;
  tracks: { href: string, total: number };
  type: string;
  uri: string;
};

/** https://developer.spotify.com/documentation/web-api/reference/get-a-list-of-current-users-playlists **/
const getUserPlaylistsInner = (token: SpotifyToken, url: string, items<Playlist[]>): Promise<Playlist[]> => {
  return fetch(url, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      "Authorization": "Bearer " + token.accessToken
    }
  }).then((r) => r.json())
    .then((r) => {
      if (r.next) { // check for more pages since we're only able to get playlists in batches of 50
        return getUserPlaylistsInner(token, r.next, items.concat(r.items));
      } else {
        return items.concat(r.items);
      }
    });
};

const getUserPlaylists = (token: SpotifyToken): Promise<Playlist[]> => {
  return getUserPlaylistsInner(
    token,
    "https://api.spotify.com/v1/me/playlists?limit=50&offset=0",
    []
  );
};

// TODO https://developer.spotify.com/documentation/web-api/reference/create-playlist
// TODO https://developer.spotify.com/documentation/web-api/reference/add-tracks-to-playlist

export default {
  makeToken, isValidToken, redirect, clearToken, // auth functions
  getUserPlaylists
};
