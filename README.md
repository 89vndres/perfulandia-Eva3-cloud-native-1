# 🌸 Perfulandia – Plataforma de Perfumería Cloud Native

Proyecto desarrollado para la asignatura **DSY1107 – Desarrollo Cloud Native I**, correspondiente a la Evaluación Parcial N.º 6.

Perfulandia es una aplicación web de venta de perfumes que integra frontend, backend, autenticación y servicios de mensajería para implementar una arquitectura basada en tecnologías Cloud Native.

---

## 1. Tecnologías utilizadas

| Tecnología | Función |
|---|---|
| React | Desarrollo del frontend |
| React Router DOM | Navegación entre páginas |
| React Bootstrap / Bootstrap | Componentes y estilos de interfaz |
| Framer Motion | Animaciones e interacciones visuales |
| Java / Spring Boot | Desarrollo del backend |
| Keycloak | Autenticación y gestión de identidad |
| OAuth 2.0 / OpenID Connect | Protocolo de autenticación |
| Kong Gateway | API Gateway y administración de rutas |
| RabbitMQ | Mensajería asíncrona |
| Apache Kafka | Streaming y procesamiento de eventos |
| Kafka UI | Visualización y administración de Kafka |
| Docker | Contenedorización |
| Docker Compose | Orquestación de servicios |

---

## 2. Arquitectura del proyecto

```text
                 USUARIO
                    |
                    v
             FRONTEND REACT
              localhost:3000
                    |
                    v
               KONG GATEWAY
              localhost:8000
                    |
                    v
             BACKEND SPRING BOOT
              localhost:8080
                    |
          +---------+---------+
          |                   |
          v                   v
       KEYCLOAK           RABBITMQ
      Autenticación       Mensajería
                              |
                    +---------+---------+
                    |         |         |
                    v         v         v
                 Órdenes  Inventario  Pagos

                    BACKEND
                       |
                       v
                  APACHE KAFKA
                       |
                       v
                    KAFKA UI
                 localhost:8085
```

---

## 3. Requisitos previos

Antes de ejecutar el proyecto, se recomienda tener instalado:

- Docker Desktop.
- Docker Compose.
- Node.js y npm.
- Java y Maven, si se desea ejecutar el backend fuera de Docker.
- Git.

Verificar las instalaciones:

```bash
docker --version
docker compose version
node --version
npm --version
```

---

## 4. Estructura del proyecto

```text
Perfulandia-main/
├── backend/
│   └── src/
├── react-perfumelandia/
│   ├── public/
│   ├── src/
│   │   ├── components/
│   │   ├── context/
│   │   ├── pages/
│   │   └── services/
│   └── package.json
├── infra/
│   └── kong/
│       └── kong.yml
├── docker-compose.yml
└── README.md
```

*La estructura puede variar según los archivos presentes en la versión final del proyecto.*

---

## 5. Instalación y ejecución del frontend

### Paso 1. Entrar a la carpeta del frontend

Desde la raíz del proyecto, ejecutar:

```bash
cd react-perfumelandia
```

### Paso 2. Instalar las dependencias

Ejecutar los siguientes comandos:

```bash
npm install
npm install react-router-dom react-bootstrap bootstrap
npm install framer-motion
```

Estos paquetes se utilizan para:

- **react-router-dom:** administrar la navegación y las rutas de la aplicación.
- **react-bootstrap:** utilizar componentes de interfaz basados en Bootstrap.
- **bootstrap:** aplicar los estilos CSS de Bootstrap.
- **framer-motion:** incorporar animaciones a los componentes de React.

Si Bootstrap todavía no está importado en el punto de entrada del frontend, agregar esta línea una sola vez en `src/index.js` o `src/main.jsx`, según la estructura del proyecto:

```javascript
import 'bootstrap/dist/css/bootstrap.min.css';
```

### Paso 3. Iniciar React

```bash
npm start
```

Si el proyecto utiliza Vite, iniciar con:

```bash
npm run dev
```

Abrir la dirección que indique la terminal. En la configuración utilizada durante el desarrollo, el frontend se ejecuta en:

```text
http://localhost:3000
```

---

## 6. Ejecución de los servicios con Docker Compose

Volver a la carpeta raíz del proyecto, donde se encuentra `docker-compose.yml`:

```bash
cd ..
docker compose up -d
```

Comprobar los contenedores:

```bash
docker compose ps
```

Consultar los registros:

```bash
docker compose logs -f
```

Para revisar únicamente el backend:

```bash
docker compose logs -f backend
```

Para detener los servicios:

```bash
docker compose down
```

---

## 7. Acceso a los servicios

Las direcciones locales utilizadas durante el desarrollo son:

| Servicio | Dirección |
|---|---|
| Frontend React | http://localhost:3000 |
| Backend Spring Boot | http://localhost:8080 |
| Kong Gateway | http://localhost:8000 |
| Kong Admin API | http://localhost:8001 |
| Keycloak | http://localhost:8081 |
| RabbitMQ Management | http://localhost:15672 |
| Kafka UI | http://localhost:8085 |

Los servicios deben estar levantados para acceder a ellos.

---

## 8. Autenticación con Keycloak

El proyecto utiliza Keycloak para administrar la autenticación de usuarios.

Configuración utilizada durante el desarrollo:

- Realm: `perfulandia`
- Cliente: `perfulandia-frontend`
- URL de Keycloak: `http://localhost:8081`
- URL del frontend: `http://localhost:3000`
- Protocolo: OAuth 2.0 / OpenID Connect

El frontend utiliza el proceso de autenticación para obtener información del usuario y el token correspondiente.

Los roles de aplicación considerados son:

- `admin`
- `user`

La autorización debe verificarse mediante los roles y tokens configurados en el proyecto.

---

## 9. API Gateway con Kong

Kong funciona como punto de entrada para las solicitudes del frontend hacia el backend.

La configuración de rutas se encuentra en:

```text
infra/kong/kong.yml
```

Entre las rutas configuradas se consideran:

- `/api`
- `/auth`
- `/actuator`
- `/perfumes`
- `/ordenes`

El Gateway permite centralizar el acceso a los endpoints y aplicar políticas como CORS.

Dirección del Gateway:

```text
http://localhost:8000
```

---

## 10. Mensajería con RabbitMQ

RabbitMQ permite implementar comunicación asíncrona entre componentes del backend.

Colas consideradas:

| Cola principal | Cola de mensajes fallidos |
|---|---|
| `perfulandia.ordenes.queue` | `perfulandia.ordenes.dlq` |
| `perfulandia.inventario.queue` | `perfulandia.inventario.dlq` |
| `perfulandia.pagos.queue` | `perfulandia.pagos.dlq` |

Las Dead Letter Queues (DLQ) permiten gestionar mensajes que no pudieron procesarse correctamente.

Para revisar las colas:

```bash
docker compose exec rabbit1 rabbitmqctl list_queues name messages_ready messages_unacknowledged consumers
```

Para revisar los consumidores:

```bash
docker compose exec rabbit1 rabbitmqctl list_consumers
```

La existencia de las colas no implica por sí sola que todos los consumidores estén procesando mensajes; esto debe verificarse mediante pruebas y registros.

---

## 11. Streaming con Apache Kafka

Apache Kafka se incorpora para la publicación, distribución y consumo de eventos.

Topic identificado durante el desarrollo:

```text
perfulandia.facturacion.eventos
```

Kafka UI permite visualizar los brokers, topics, particiones, mensajes y grupos de consumidores disponibles.

Dirección:

```text
http://localhost:8085
```

Para revisar los servicios Kafka:

```bash
docker compose ps
```

Para revisar los registros de Kafka:

```bash
docker compose logs kafka1
```

Para revisar los registros del backend:

```bash
docker compose logs --tail=200 backend
```

La cantidad de brokers, nodos ZooKeeper, topics, particiones y réplicas debe corresponder a la configuración efectiva de `docker-compose.yml` y Kafka. No se debe asumir que la arquitectura objetivo de alta disponibilidad está implementada sin verificarla.

---

## 12. Verificación del funcionamiento

Para comprobar el estado general:

```bash
docker compose ps
```

Para revisar los registros del backend:

```bash
docker compose logs --tail=200 backend
```

Para verificar RabbitMQ:

```bash
docker compose exec rabbit1 rabbitmqctl list_queues name messages_ready messages_unacknowledged consumers
```

Para comprobar Kafka:

1. Ingresar a Kafka UI.
2. Revisar los brokers disponibles.
3. Revisar los topics.
4. Verificar las particiones y réplicas.
5. Revisar los mensajes publicados.
6. Revisar los grupos de consumidores y sus offsets.

También se debe realizar una prueba funcional desde React, incluyendo inicio de sesión y ejecución de una compra.

---

## 13. Evidencias de funcionamiento

Para documentar la evaluación, se recomienda incorporar capturas de pantalla de:

1. Aplicación React funcionando.
2. Contenedores activos mediante `docker compose ps`.
3. Compra realizada correctamente.
4. Keycloak y usuarios registrados.
5. Kong y sus rutas configuradas.
6. RabbitMQ y sus colas principales y DLQ.
7. Kafka UI mostrando brokers y topics.
8. Mensajes publicados y consumidos.
9. Particiones, réplicas y grupos de consumidores.
10. Logs del backend mostrando el procesamiento de eventos.

Las capturas deben corresponder a pruebas reales del proyecto.

---

## 14. Objetivos del proyecto

- Integrar frontend y backend.
- Implementar autenticación centralizada.
- Administrar el acceso a los servicios mediante API Gateway.
- Incorporar mensajería asíncrona.
- Implementar procesamiento de eventos con Kafka.
- Utilizar Docker Compose para administrar los servicios.
- Comprender los conceptos de escalabilidad, disponibilidad y tolerancia a fallos.

---

## 15. Autoría

**Proyecto:** Perfulandia  
**Asignatura:** DSY1107 – Desarrollo Cloud Native I  
**Evaluación:** Parcial N.º 6  
**Institución:** Duoc UC

---



**Perfulandia – Desarrollo Cloud Native I.**
