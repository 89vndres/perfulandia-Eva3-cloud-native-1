# Perfulandia — preparación para Evaluación Parcial N°5 (DSY1107)

## Alcance
Se conserva React (según autorización del docente), Spring Boot, RabbitMQ y Docker Compose. Kafka se integra con dos tópicos: `perfulandia.ordenes.eventos` y `perfulandia.facturacion.eventos`.

## Matriz de la pauta EP5
- Cliente Kafka (12%): bootstrap servers, serializers/deserializers JSON, grupos, ACKs, idempotencia, particiones y réplicas en `application.properties` y `docker-compose.yml`.
- DTO, productores y consumidores para ambos tópicos (12% + 14% + 14%): `OrdenEvento`, `FacturacionEvento`, `KafkaEventProducer`, `OrdenKafkaConsumer` y `FacturacionKafkaConsumer`. La creación de una orden publica eventos de orden y facturación.
- Administrador Kafka (6% + 6%): `KafkaAdminController` y `KafkaAdminService` permiten listar, crear, actualizar configuración, eliminar tópicos y consultar grupos/detalles.
- Errores (12%): validación de payload, logs contextuales, reintentos configurados en `KafkaListenerConfig` y registro de eventos descartados después de agotar reintentos.
- Parámetros de tópicos (12%): retención, cleanup policy, particiones y factor de réplica; el clúster local usa 2 brokers y factor de réplica 2.
- Consumer groups (12%): cada flujo de consumo tiene un `groupId` específico y el administrador permite inspeccionar estado y miembros.

## Identidad y autorización
React utiliza Keycloak Authorization Code + PKCE (`keycloak-js`). Spring valida tokens de Keycloak usando el JWKS del realm. Los endpoints `/api/admin/kafka/**` y `/api/admin/rabbitmq/**` exigen rol `admin` del realm. El login HMAC anterior se mantiene temporalmente para compatibilidad de usuarios existentes.

Para dar permisos de administrador a una cuenta de demostración, asigna el rol `admin` desde la consola de Keycloak (Realm roles → Users → Role mapping). El registro público solo crea usuarios normales.

## Inicio local en Windows
1. Copia `.env.example` a `.env` en la raíz y reemplaza secretos/contraseñas antes de usarlo fuera de una demo local.
2. En una terminal, desde la raíz ejecuta `docker compose up --build -d`.
3. Comprueba `docker compose ps` y `docker compose logs -f backend`.
4. En otra terminal: `cd react-perfumelandia`, copia `.env.example` a `.env`, ejecuta `npm install` y `npm start`.
5. Abre React en `http://localhost:3000`, Keycloak en `http://localhost:8081` y Kafka UI en `http://localhost:8085`.

## Pruebas manuales Kafka
- Listar tópicos (requiere admin): `GET http://localhost:8080/api/admin/kafka/topics`
- Crear: `POST /api/admin/kafka/topics` con `{ "name":"demo.eventos", "partitions":3, "replicas":2, "retentionMs":604800000, "cleanupPolicy":"delete" }`.
- Actualizar: `PUT /api/admin/kafka/topics/demo.eventos` con `{ "retentionMs":86400000, "cleanupPolicy":"compact", "partitions":4 }`.
- Eliminar: `DELETE /api/admin/kafka/topics/demo.eventos`.
- Consumer groups: `GET /api/admin/kafka/consumer-groups` y `GET /api/admin/kafka/consumer-groups/perfulandia-ordenes-consumer`.
- Publicar orden: `POST /api/kafka/events/orders` con `{ "orderId":1001, "email":"demo@perfulandia.cl", "total":25990 }`.
- Publicar facturación: `POST /api/kafka/events/billing` con `{ "orderId":1001, "email":"demo@perfulandia.cl", "amount":25990, "currency":"CLP" }`.

Los endpoints administrativos y de eventos requieren `Authorization: Bearer <token>` y rol admin para administración. En React el token se adjunta automáticamente en las llamadas que usan `apiFetch`.

## Límites que deben resolverse con el equipo/docente
- La pauta solicita base de datos cloud. El backend ahora admite PostgreSQL mediante driver y variables `SPRING_DATASOURCE_*`, pero el perfil predeterminado sigue siendo H2 local. Para completar la conexión real se requiere proveedor, URL, usuario, contraseña y reglas de red; configura esos valores en `.env` antes de desplegar.
- La pauta formal menciona Angular, pero se mantiene React por autorización expresa del docente.
- La estructura actual es un backend Spring Boot modular; si el docente exige procesos desplegados como varios microservicios independientes, será necesario separar servicios y despliegues, no basta con paquetes Java.
- Verifica que el realm importado se aplique desde cero: Keycloak no siempre sobrescribe un realm persistido previamente en su volumen.
