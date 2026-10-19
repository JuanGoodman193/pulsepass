# PulsePass

PulsePass es una plataforma académica para gestionar eventos, artistas, usuarios y entradas. Combina la capa de persistencia del MVP definido en `PRD_PulsePass.md` con la capa de servicios del taller posterior, usando Java 21, Spring Boot 4, Spring Data JPA, Flyway, PostgreSQL y MapStruct.

## Alcance

El proyecto cubre persistencia, integridad relacional, consultas y casos de uso de servicio para venues, artistas, eventos, usuarios y tickets. La capa Service aplica reglas de negocio, coordina repositories, controla transacciones y expone DTOs mapeados con MapStruct. No incluye API REST, frontend, Spring Security, pasarela de pagos ni Testcontainers para las pruebas unitarias de servicios.

## Modelo

Las entidades son `Venue`, `Event`, `Artist`, `User`, `UserProfile` y `Ticket`.

- `Venue` 1:N `Event`
- `Event` N:M `Artist` mediante `event_artists`
- `User` 1:1 `UserProfile`
- `User` 1:N `Ticket`
- `Event` 1:N `Ticket`

Los enums se almacenan con `EnumType.STRING` y los precios utilizan `BigDecimal`/`NUMERIC(12, 2)`. PostgreSQL protege las reglas de unicidad, referencias y rangos mediante `UNIQUE`, `FK` y `CHECK`.

## Migraciones

- `V1__create_schema.sql`: crea tablas, relaciones, constraints e índices.
- `V2__insert_initial_artists.sql`: carga Solar Beat, Neon Waves, Caribbean Sound, Ocean Drive y Digital Pulse.
- `V3__add_streaming_url_to_event.sql`: agrega la URL opcional de streaming sin modificar V1.

Hibernate usa `spring.jpa.hibernate.ddl-auto=validate`; Flyway es el único responsable de crear y evolucionar el esquema.

## Consultas implementadas

Los repositories usan métodos heredados, Query Methods y JPQL sin SQL nativo:

- Eventos publicados ordenados por fecha y eventos de un venue.
- Eventos por artista, ciudad y artista, y recomendaciones case-insensitive.
- Usuarios por email ignorando mayúsculas.
- Tickets por email/estado, tickets pagados por evento y conteo de ventas.
- Tickets de eventos futuros ordenados cronológicamente.

## Capa de servicios

Cada servicio tiene una interfaz y una implementación. Las lecturas usan transacciones de solo lectura y las escrituras son transaccionales. Las reglas de negocio producen excepciones de dominio y los contratos usan DTOs inmutables implementados como `record`.

La compra de entradas valida usuario, evento, edad y capacidad; calcula el precio dentro del sistema y actualiza el evento a `SOLD_OUT` en la misma transacción cuando se vende el último cupo.

## Capa de Controladores REST (Fase 3)

La capa de controladores expone la API REST de PulsePass mediante Spring MVC, Bean Validation y `@RestControllerAdvice`:

- **VenueController** (`/api/venues`):
  - `GET /api/venues/{code}`: Consulta de venue por código (200 OK / 404 Not Found).
  - `GET /api/venues/active`: Listado de venues activos (200 OK).
- **EventController** (`/api/events`):
  - `POST /api/events`: Creación de evento en estado DRAFT (201 Created / 400 Bad Request / 409 Conflict).
  - `GET /api/events/{eventCode}`: Consulta de evento por código (200 OK / 404 Not Found).
  - `GET /api/events/published`: Listado de eventos en estado PUBLISHED (200 OK).
  - `PATCH /api/events/{eventCode}/publish`: Publicación de evento DRAFT a PUBLISHED (200 OK / 409 Conflict).
  - `POST /api/events/{eventCode}/artists/{artistId}`: Asociación de artista a evento (200 OK / 409 Conflict).
  - `GET /api/events/by-artist?stageName=...`: Búsqueda de eventos por nombre artístico (200 OK).
- **ArtistController** (`/api/artists`):
  - `GET /api/artists/{id}`: Consulta de artista por ID (200 OK / 404 Not Found).
  - `GET /api/artists/by-stage-name?stageName=...`: Consulta por nombre artístico (200 OK / 404 Not Found).
  - `GET /api/artists/active`: Listado de artistas activos (200 OK).
- **UserController** (`/api/users`):
  - `POST /api/users`: Registro de usuario (201 Created / 400 Bad Request / 409 Conflict).
  - `GET /api/users/by-email?email=...`: Consulta de usuario por email (200 OK / 404 Not Found).
  - `GET /api/users/by-username?username=...`: Consulta de usuario por username (200 OK / 404 Not Found).
- **TicketController** (`/api`):
  - `POST /api/tickets`: Compra de entrada en estado PAID (201 Created / 400 Bad Request / 404 Not Found / 409 Conflict).
  - `GET /api/tickets/{ticketCode}`: Consulta de entrada por código (200 OK / 404 Not Found).
  - `GET /api/tickets/by-user?email=...`: Consulta de entradas de un usuario (200 OK).
  - `GET /api/events/{eventCode}/tickets/paid`: Consulta de entradas PAID de un evento (200 OK).
  - `PATCH /api/tickets/{ticketCode}/cancel`: Cancelación de entrada PAID a CANCELLED (200 OK / 409 Conflict).
  - `PATCH /api/tickets/{ticketCode}/use`: Validación y uso de entrada PAID a USED (200 OK / 409 Conflict).

### Manejo Global de Errores

Todos los errores HTTP responden con el contrato uniforme `ErrorResponse`:
- `400 Bad Request`: Validaciones estructurales fallidas (Bean Validation) o JSON mal formado.
- `404 Not Found`: Recursos no encontrados (`ResourceNotFoundException`).
- `409 Conflict`: Conflictos de unicidad (`DuplicateResourceException`) o reglas de negocio incumplidas (`BusinessRuleException`).
- `500 Internal Server Error`: Errores internos no controlados sin exponer stack traces ni detalles sensibles.

## Ejecutar

Requisitos: JDK 21, Maven 3.9+ y PostgreSQL para ejecutar la aplicación. Las pruebas de integración de persistencia requieren Docker porque Testcontainers inicia PostgreSQL automáticamente; las pruebas unitarias de servicios y de controladores no requieren base de datos.

```bash
mvn spring-boot:run
```

Variables opcionales:

```text
DB_URL=jdbc:postgresql://localhost:5432/pulsepass
DB_USER=postgres
DB_PASSWORD=postgres
```

## Pruebas

## Capa de Seguridad con Spring Security y JJWT (Fase 4)

La Fase 4 incorpora una arquitectura de seguridad stateless basada en **Spring Security** y **JJWT (Java JWT 0.13.0)**, con autenticación mediante Bearer token y autorización basada en roles (`USER`, `STAFF`, `ADMIN`) y reglas de propiedad de recursos (anti-IDOR).

### Nuevos Endpoints de Autenticación (`/api/auth`)

- **`POST /api/auth/login`**: Autentica mediante credenciales (`identifier`: username o email, `password`). Emite un JWT firmado con HS256 si las credenciales son válidas (`200 OK` con `AuthResponse`: `accessToken`, `tokenType: "Bearer"`, `expiresIn`). Si las credenciales fallan, retorna `401 Unauthorized` genérico sin revelar la existencia del usuario.
- **`GET /api/auth/me`**: Requiere autenticación Bearer válida. Retorna el perfil del usuario autenticado (`200 OK` con `AuthenticatedUserResponse`: `id`, `username`, `email`, `roles`).

### Registro Atómico de Usuarios (`POST /api/users`)

- Incorpora el campo `password` obligatorio en `RegisterUserRequest` (mínimo 12 caracteres, máximo 72 bytes para compatibilidad con BCrypt).
- Realiza el hashing con `BCryptPasswordEncoder` (10 rounds).
- Guarda de forma atómica en una única transacción: `User`, `UserProfile`, `UserCredential` (hash de contraseña) y asigna el rol inicial `ROLE_USER` en `user_roles`.
- No emite JWT durante el registro: el flujo obligatorio es registro y posterior inicio de sesión.
- No expone hashes, contraseñas ni secretos en la respuesta (`UserResponse`).

### Matriz de Autorización de las 20 Operaciones Heredadas + Auth

| Método y Ruta | Acceso Exigido | Regla / Detalle |
|---|---|---|
| `POST /api/auth/login` | Público | Autenticación y emisión de JWT |
| `GET /api/auth/me` | Autenticado | Perfil del principal autenticado |
| `GET /api/venues/{code}` | Público | Consulta de venue activo |
| `GET /api/venues/active` | Público | Listado de venues activos |
| `POST /api/events` | `ADMIN` | Creación de evento en estado DRAFT |
| `GET /api/events/{eventCode}` | Público / `STAFF` / `ADMIN` | Público solo si PUBLISHED; STAFF/ADMIN pueden ver otros estados |
| `GET /api/events/published` | Público | Listado exclusivo de eventos PUBLISHED |
| `PATCH /api/events/{eventCode}/publish` | `ADMIN` | Publicación de evento |
| `POST /api/events/{eventCode}/artists/{artistId}` | `ADMIN` | Asociación de artista a evento |
| `GET /api/events/by-artist?stageName=...` | Público / `STAFF` / `ADMIN` | Público solo si PUBLISHED; STAFF/ADMIN pueden ver otros estados |
| `GET /api/artists/{id}` | Público | Consulta de artista |
| `GET /api/artists/by-stage-name?stageName=...` | Público | Consulta por nombre artístico |
| `GET /api/artists/active` | Público | Listado de artistas activos |
| `POST /api/users` | Público | Registro de usuario (asigna exclusivamente `USER`) |
| `GET /api/users/by-email?email=...` | Propietario o `ADMIN` | USER solo consulta su propio email; 403 para emails ajenos |
| `GET /api/users/by-username?username=...` | Propietario o `ADMIN` | USER solo consulta su propio username; 403 para usernames ajenos |
| `POST /api/tickets` | `USER` | `PurchaseTicketRequest.userEmail` debe pertenecer al principal; 403 para emails ajenos |
| `GET /api/tickets/{ticketCode}` | Propietario o `STAFF`/`ADMIN` | USER solo consulta entradas propias en base de datos; 403 para ajenas |
| `GET /api/tickets/by-user?email=...` | Propietario o `ADMIN` | USER solo consulta sus propias entradas; 403 para otros usuarios |
| `GET /api/events/{eventCode}/tickets/paid` | `STAFF` / `ADMIN` | Operación operativa para verificar asistentes pagados |
| `PATCH /api/tickets/{ticketCode}/cancel` | Propietario `USER` o `ADMIN` | USER solo cancela sus propias entradas; 403 para entradas ajenas |
| `PATCH /api/tickets/{ticketCode}/use` | `STAFF` / `ADMIN` | Validación y uso operativo de entradas en puerta |

### Protección de Propiedad y Prevención de IDOR

1. **Compra de entradas (`POST /api/tickets`)**: Un usuario con rol `USER` únicamente puede comprar entradas asociadas a su propio correo. Si `userEmail` difiere del correo del principal autenticado, se rechaza con `403 Forbidden` antes de ejecutar la lógica de negocio.
2. **Consulta y cancelación de entradas**: Se verifica el propietario real persistido en la base de datos contra el ID del principal antes de retornar información o mutar el estado a `CANCELLED`.
3. **Visibilidad de eventos en borrador**: Las rutas públicas `GET /api/events/{eventCode}` y `GET /api/events/by-artist` filtran los eventos no publicados (`DRAFT`), impidiendo la fuga de información preliminar. Los roles `STAFF` y `ADMIN` conservan el acceso a eventos en cualquier estado.

### Configuración de Variables de Entorno y Secretos

La aplicación lee las siguientes propiedades (ver `.env.example`):

```bash
# Clave secreta Base64 (MANDATORIA: mínimo 32 bytes / 256 bits tras decodificar Base64)
PULSEPASS_JWT_SECRET_BASE64=TW9ja0tleUZvclB1bHNlUGFzc1NlY3VyaXR5VGVzdHMzMkJ5dGVzPQ==

# Emisor del token (opcional, predeterminado: pulsepass-api)
PULSEPASS_JWT_ISSUER=pulsepass-api

# Duración del Access Token (opcional, formato ISO-8601, predeterminado: PT15M = 15 minutos)
PULSEPASS_JWT_ACCESS_TOKEN_TTL=PT15M
```

> **Validación estricta de inicio:** La aplicación falla inmediatamente al arrancar si `PULSEPASS_JWT_SECRET_BASE64` falta, está vacía, no es Base64 válido o contiene menos de 32 bytes (256 bits). Nunca se codifican secretos reales en el repositorio ni en archivos versionados.

### Migraciones Flyway Añadidas

- `V4__security_credentials_roles.sql`:
  - Crea la tabla `user_credentials` (1:1 con `users`) con columnas `user_id` (PK/FK a `users.id`), `password_hash VARCHAR(255) NOT NULL` y `password_changed_at TIMESTAMP NOT NULL`.
  - Crea la tabla `user_roles` con clave primaria compuesta `(user_id, role)` con FK a `users.id` y constraint `CHECK (role IN ('USER', 'STAFF', 'ADMIN'))`.
  - Índice optimizado `idx_user_roles_user_id` para búsquedas ágiles de autoridades.

### Política de Usuarios Legados

Los usuarios registrados en migraciones o fases anteriores sin credenciales permanecen en la base de datos sin contraseña por defecto ni contraseñas compartidas. `CustomUserDetailsService` rechaza automáticamente el inicio de sesión para cuentas sin registro en `user_credentials`, garantizando que no puedan ser vulneradas hasta que se aprovisionen credenciales seguras mediante procesos administrativos.

### Semilla de Usuarios y Roles de Demostración

Para pruebas locales o entornos de desarrollo, se pueden aprovisionar usuarios con roles privilegiados (`STAFF`, `ADMIN`) de forma segura mediante un script SQL administrativo o carga de seed controlada:

```sql
-- Ejemplo administrativo local: Asignar rol STAFF a un usuario existente (ID: 2)
INSERT INTO user_roles (user_id, role) VALUES (2, 'STAFF') ON CONFLICT DO NOTHING;

-- Ejemplo administrativo local: Asignar rol ADMIN a un usuario existente (ID: 3)
INSERT INTO user_roles (user_id, role) VALUES (3, 'ADMIN') ON CONFLICT DO NOTHING;
```

El registro público (`POST /api/users`) tiene estrictamente bloqueada la asignación de roles distintos de `USER`.

### Limitaciones Fuera del Alcance (MVP)

Conforme a la especificación del PRD para esta fase, las siguientes características están explícitamente fuera del alcance:
- Refresh tokens y rotación de tokens.
- Flujo de recuperación o restablecimiento de contraseña.
- Autenticación multifactor (MFA / 2FA).
- Single Sign-On (SSO / OAuth2 social login).
- Revocación distribuida instantánea de JWT (blacklist en Redis).
- Limitación distribuida de tasa de intentos de login (Rate limiting).

## Ejecutar

Requisitos: JDK 21, Maven 3.9+ y PostgreSQL para ejecutar la aplicación. Las pruebas de integración de persistencia requieren Docker porque Testcontainers inicia PostgreSQL automáticamente; las pruebas unitarias de servicios, controladores y seguridad HTTP no requieren base de datos externa.

```bash
mvn spring-boot:run
```

## Pruebas

Para ejecutar las pruebas de seguridad HTTP (20 escenarios T-SEC-001 a T-SEC-020):

```bash
mvn test -Dtest=SecurityHttpTest
```

Para ejecutar las pruebas unitarias de la capa de seguridad (`JwtService`, `AuthService`, `CustomUserDetailsService`, `AuthorizationService`, `JwtAuthenticationFilter`):

```bash
mvn test -Dtest="*Security*Test,Jwt*Test,Auth*Test,CustomUser*Test"
```

Para ejecutar todas las pruebas unitarias, de controladores y de seguridad (sin requerir Docker):

```bash
mvn test -Dtest="!PersistenceIntegrationTest"
```

Para ejecutar la suite completa incluyendo integración con PostgreSQL real (requiere Docker activo):

```bash
mvn clean test
```

Las pruebas no usan H2. `PersistenceIntegrationTest` verifica las migraciones Flyway, asociaciones JPA, Query Methods, JPQL, conteo de tickets pagados y restricciones reales de PostgreSQL mediante Testcontainers. Las pruebas unitarias de servicio (`*ServiceImplTest`) validan las reglas de negocio aisladas con Mockito. Las pruebas de controladores (`*ControllerTest`) validan serialización JSON y manejo de excepciones web. Las pruebas de seguridad HTTP (`SecurityHttpTest`) ejercitan de punta a punta la autenticación con token Bearer, autorización por roles (`USER`, `STAFF`, `ADMIN`), restricciones anti-IDOR y formato uniforme de errores (`ErrorResponse`).
