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

Para ejecutar las pruebas unitarias de servicios y controladores (sin requerir Docker):

```bash
mvn test -Dtest="*ServiceImplTest,*ControllerTest"
```

O específicamente las pruebas de la capa de controladores:

```bash
mvn test -Dtest="*ControllerTest"
```

Para ejecutar la suite completa (requiere Docker Desktop activo para Testcontainers de PostgreSQL):

```bash
mvn clean test
```

Las pruebas no usan H2. `PersistenceIntegrationTest` verifica las migraciones Flyway, asociaciones JPA, Query Methods, JPQL, conteo de tickets pagados y una restricción `UNIQUE` real de PostgreSQL con Testcontainers. Los tests de servicio prueban las reglas de negocio aisladas con mocks de Mockito. Las pruebas de controlador utilizan `@WebMvcTest`, MockMvc y `@MockitoBean` para validar contratos HTTP, serialización JSON, códigos de estado y manejo de excepciones sin levantar base de datos ni ApplicationContext completo.
