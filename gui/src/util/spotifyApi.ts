/** Return token if it is still valid. If it is invalid
retrun nil and remove the localstorage entry. **/
const isValidToken (token: string): number | void => {
  if (token) {
    if (new Date < token.expires_at) {
      return token;
    } else {
      window.localStorage.removeItem("spotify-token");
      return null;
    }
  }
};

const redirect (clientId: string): void => {

};
