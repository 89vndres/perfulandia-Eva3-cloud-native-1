import { useState } from 'react';
import { Container, Button, Alert } from 'react-bootstrap';
import { useAuth } from '../context/AuthContext';

export default function Login() {
    const { login } = useAuth();
    const [error, setError] = useState('');
    const handleLogin = async () => {
        try { await login(); } catch (e) { console.error(e); setError('No fue posible abrir el inicio de sesión. Verifica que Keycloak esté activo.'); }
    };
    return <Container className="mt-5" style={{ maxWidth: '440px' }}>
        <h2 className="text-center">Iniciar Sesión</h2>
        {error && <Alert variant="danger">{error}</Alert>}
        <p className="text-center">Accede de forma segura mediante Perfulandia ID.</p>
        <Button className="w-100" onClick={handleLogin}>Ingresar con Keycloak</Button>
    </Container>;
}
