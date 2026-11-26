
import keycloak from './keycloak';

// Todas las peticiones pasan por Kong
export const API_BASE_URL =
    process.env.REACT_APP_API_URL || 'http://localhost:8000';

export async function apiFetch(url, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...(options.headers || {})
    };

    // Agregar token JWT de Keycloak
    if (keycloak.authenticated) {
        try {
            await keycloak.updateToken(30);
        } catch (error) {
            keycloak.login();
            throw error;
        }

        if (keycloak.token) {
            headers.Authorization = `Bearer ${keycloak.token}`;
        }
    }

    // Construir URL usando Kong como API Gateway
    const requestUrl = /^https?:\/\//i.test(url)
        ? url
        : `${API_BASE_URL}${url.startsWith('/') ? '' : '/'}${url}`;

    const response = await fetch(requestUrl, {
        ...options,
        headers
    });

    // Si el token expiró o no es válido
    if (response.status === 401 && keycloak.authenticated) {
        keycloak.clearToken();
        keycloak.login();
    }

    return response;
}
