
import {
    createContext,
    useContext,
    useCallback,
    useEffect,
    useMemo,
    useState
} from 'react';

import keycloak, {
    initializeKeycloak
} from '../services/keycloak';

const AuthContext = createContext(null);

// Obtener los roles del usuario desde Keycloak
function getUserRoles(profile) {
    const realmRoles = profile?.realm_access?.roles || [];

    const clientRoles = Object.values(
        profile?.resource_access || {}
    ).flatMap(client => client.roles || []);

    return [...new Set([...realmRoles, ...clientRoles])];
}

// Construir el usuario autenticado
function readUser() {
    if (!keycloak.authenticated || !keycloak.tokenParsed) {
        return null;
    }

    const profile = keycloak.tokenParsed;
    const roles = getUserRoles(profile);

    const isAdmin = roles.some(
        role => String(role).trim().toLowerCase() === 'admin'
    );

    const user = {
        id: profile.sub || '',
        username: profile.preferred_username || '',
        email: profile.email || '',
        name:
            profile.name ||
            profile.given_name ||
            profile.preferred_username ||
            'Usuario',
        role: isAdmin ? 'admin' : 'user',
        roles: roles
    };

    console.log('========== PERFULANDIA AUTH ==========');
    console.log('Usuario:', user.username);
    console.log('Roles recibidos:', roles);
    console.log('Es administrador:', isAdmin);
    console.log('Rol asignado:', user.role);
    console.log('======================================');

    return user;
}

export function AuthProvider({ children }) {
    const [ready, setReady] = useState(false);
    const [user, setUser] = useState(null);
    const [token, setToken] = useState(null);

    useEffect(() => {
        let active = true;

        const updateAuthentication = () => {
            if (!active) return;

            setUser(readUser());
            setToken(keycloak.token || null);
        };

        initializeKeycloak()
            .then(authenticated => {
                if (!active) return;

                if (authenticated) {
                    updateAuthentication();
                } else {
                    setUser(null);
                    setToken(null);
                }

                setReady(true);
            })
            .catch(error => {
                console.error(
                    'Error inicializando Keycloak:',
                    error
                );

                if (active) {
                    setUser(null);
                    setToken(null);
                    setReady(true);
                }
            });

        keycloak.onAuthSuccess = updateAuthentication;

        keycloak.onAuthRefreshSuccess = updateAuthentication;

        keycloak.onAuthLogout = () => {
            if (!active) return;

            setUser(null);
            setToken(null);
        };

        keycloak.onTokenExpired = () => {
            keycloak.updateToken(30)
                .then(updateAuthentication)
                .catch(error => {
                    console.error('Error renovando token:', error);
                    keycloak.login();
                });
        };

        return () => {
            active = false;
        };
    }, []);

    // Iniciar sesión
    const login = useCallback(async () => {
        return keycloak.login({
            redirectUri: window.location.origin + '/productos'
        });
    }, []);

    // Registrar usuario
    const register = useCallback(async () => {
        return keycloak.register({
            redirectUri: window.location.origin + '/'
        });
    }, []);

    // Cerrar sesión
    const logout = useCallback(async () => {
        return keycloak.logout({
            redirectUri: window.location.origin
        });
    }, []);

    const value = useMemo(() => ({
        user,
        token,
        ready,
        login,
        register,
        logout,
        isAdmin: user?.role === 'admin'
    }), [user, token, ready, login, register, logout]);

    if (!ready) {
        return (
            <div style={{
                minHeight: '40vh',
                display: 'grid',
                placeItems: 'center'
            }}>
                Conectando con Perfulandia ID...
            </div>
        );
    }

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    return useContext(AuthContext);
}
