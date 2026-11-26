import { useState } from 'react';
import { Modal, Form, Button } from 'react-bootstrap';
import { useAuth } from '../context/AuthContext';

export default function AuthModal({ show, handleClose }) {
    const [isLogin, setIsLogin] = useState(true);
    const { login, register } = useAuth();

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            if (isLogin) await login();
            else await register();
        } catch (error) {
            console.error('No fue posible iniciar el flujo de identidad', error);
            alert('No fue posible conectar con Keycloak. Verifica que el servicio esté iniciado.');
        }
    };

    const modalStyle = {
        backgroundColor: '#0a0a0a',
        color: '#fff',
        border: '1px solid #d4af37',
        borderRadius: '20px'
    };

    const activeTab = {
        color: '#d4af37',
        borderBottom: '2px solid #d4af37',
        fontWeight: 'bold'
    };

    const inactiveTab = {
        color: '#888',
        cursor: 'pointer'
    };

    return (
        <Modal 
            show={show} 
            onHide={handleClose} 
            centered
            contentClassName="bg-transparent border-0" 
        >
            <div style={modalStyle}>
                <Modal.Header closeButton closeVariant="white" style={{ borderBottom: '1px solid #333' }}>
                    <Modal.Title style={{ fontFamily: "'Playfair Display', serif" }}>
                        {isLogin ? 'Bienvenido de nuevo' : 'Únete a Perfulandia'}
                    </Modal.Title>
                </Modal.Header>
                
                <Modal.Body className="p-4">
                    <div className="d-flex justify-content-center mb-4 gap-4">
                        <span 
                            style={isLogin ? activeTab : inactiveTab} 
                            onClick={() => setIsLogin(true)}
                        >
                            INICIAR SESIÓN
                        </span>
                        <span 
                            style={!isLogin ? activeTab : inactiveTab} 
                            onClick={() => setIsLogin(false)}
                        >
                            CREAR CUENTA
                        </span>
                    </div>

                    <Form onSubmit={handleSubmit}>
                        <p className="text-center mb-4">{isLogin
                            ? 'Serás redirigido a Perfulandia ID para autenticarte de forma segura.'
                            : 'Crea tu cuenta desde el portal seguro de identidad Perfulandia ID.'}</p>
                        <Button type="submit" className="w-100 py-2" style={{
                            backgroundColor: '#d4af37', border: 'none', color: '#000',
                            fontWeight: 'bold', borderRadius: '10px'
                        }}>
                            {isLogin ? 'INGRESAR CON KEYCLOAK' : 'REGISTRARSE CON KEYCLOAK'}
                        </Button>
                    </Form>
                </Modal.Body>
                
                <Modal.Footer style={{ borderTop: '1px solid #333', justifyContent: 'center' }}>
                    <small className="text-white">
                        {isLogin ? "¿No tienes cuenta? " : "¿Ya tienes cuenta? "}
                        <span 
                            style={{ color: '#d4af37', cursor: 'pointer', fontWeight: 'bold' }}
                            onClick={() => setIsLogin(!isLogin)}
                        >
                            {isLogin ? 'Regístrate aquí' : 'Inicia sesión'}
                        </span>
                    </small>
                </Modal.Footer>
            </div>
        </Modal>
    );
}