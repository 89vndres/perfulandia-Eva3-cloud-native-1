import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
    url: process.env.REACT_APP_KEYCLOAK_URL || 'http://localhost:8081',
    realm: process.env.REACT_APP_KEYCLOAK_REALM || 'perfulandia',
    clientId: process.env.REACT_APP_KEYCLOAK_CLIENT_ID || 'perfulandia-frontend'
});

let initialization;
export function initializeKeycloak() {
    if (!initialization) {
        initialization = keycloak.init({
            onLoad: 'check-sso',
            pkceMethod: 'S256',
            checkLoginIframe: false,
            silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html'
        });
    }
    return initialization;
}
export default keycloak;
