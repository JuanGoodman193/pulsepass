# PulsePass — Checklist de Ejecución E2E Manual

Este documento proporciona la lista de comprobación exhaustiva para la verificación manual de la API PulsePass desde Postman Desktop / Web Agent, cubriendo las 22 rutas base (20 operaciones de negocio y 2 rutas de autenticación), los escenarios negativos, de seguridad, de propiedad y los flujos recomendados.

---

## 1. Matriz de Cobertura de las 22 Rutas Base

| # | Módulo | Método | Ruta | Rol / Política | Estado en Repositorio Actual |
| :-: | :--- | :--- | :--- | :--- | :--- |
| **1** | Venues | `GET` | `/api/venues/{code}` | `ANONYMOUS` (Público) | Implementado (Fase 3) |
| **2** | Venues | `GET` | `/api/venues/active` | `ANONYMOUS` (Público) | Implementado (Fase 3) |
| **3** | Artists | `GET` | `/api/artists/{id}` | `ANONYMOUS` (Público) | Implementado (Fase 3) |
| **4** | Artists | `GET` | `/api/artists/by-stage-name?stageName=...` | `ANONYMOUS` (Público) | Implementado (Fase 3) |
| **5** | Artists | `GET` | `/api/artists/active` | `ANONYMOUS` (Público) | Implementado (Fase 3) |
| **6** | Users | `POST` | `/api/users` | `ANONYMOUS` (Público) | Implementado (Fase 3) |
| **7** | Users | `GET` | `/api/users/by-email?email=...` | `USER` (Propio) / `ADMIN` | Implementado (Fase 3) *(Fase 4 security pendiente)* |
| **8** | Users | `GET` | `/api/users/by-username?username=...` | `USER` (Propio) / `ADMIN` | Implementado (Fase 3) *(Fase 4 security pendiente)* |
| **9** | Events | `POST` | `/api/events` | `ADMIN` | Implementado (Fase 3) *(Fase 4 security pendiente)* |
| **10** | Events | `GET` | `/api/events/{eventCode}` | `ANONYMOUS` (si publicado) / `OPS` | Implementado (Fase 3) |
| **11** | Events | `GET` | `/api/events/published` | `ANONYMOUS` (Público) | Implementado (Fase 3) |
| **12** | Events | `PATCH` | `/api/events/{eventCode}/publish` | `ADMIN` | Implementado (Fase 3) *(Fase 4 security pendiente)* |
| **13** | Events | `POST` | `/api/events/{eventCode}/artists/{artistId}` | `ADMIN` | Implementado (Fase 3) *(Fase 4 security pendiente)* |
| **14** | Events | `GET` | `/api/events/by-artist?stageName=...` | `ANONYMOUS` (Público) | Implementado (Fase 3) |
| **15** | Tickets | `POST` | `/api/tickets` | `USER` (Propio) | Implementado (Fase 3) |
| **16** | Tickets | `GET` | `/api/tickets/{ticketCode}` | `USER` (Propio) / `OPS` | Implementado (Fase 3) |
| **17** | Tickets | `GET` | `/api/tickets/by-user?email=...` | `USER` (Propio) / `ADMIN` | Implementado (Fase 3) |
| **18** | Tickets | `GET` | `/api/events/{eventCode}/tickets/paid` | `STAFF` / `ADMIN` | Implementado (Fase 3) |
| **19** | Tickets | `PATCH` | `/api/tickets/{ticketCode}/cancel` | `USER` (Propio) / `ADMIN` | Implementado (Fase 3) |
| **20** | Tickets | `PATCH` | `/api/tickets/{ticketCode}/use` | `STAFF` | Implementado (Fase 3) |
| **21** | Auth | `POST` | `/api/auth/login` | `ANONYMOUS` (Público) | **BLOQUEADO:** Dependencia Fase 4 pendiente |
| **22** | Auth | `GET` | `/api/auth/me` | Autenticado (`Bearer`) | **BLOQUEADO:** Dependencia Fase 4 pendiente |

---

## 2. Checklist por Módulos y Categorías

### Módulo 00: Preparación y Conectividad

- [ ] **[TC-CONN-001]** `GET /api/venues/active`: La aplicación responde código `200 OK` y un arreglo JSON con los venues activos.
- [ ] **[TC-CONN-002]** `GET /api/artists/active`: La aplicación responde código `200 OK` con los artistas iniciales sembrados en V2 (`Solar Beat`, `Neon Waves`, etc.).

### Módulo 01: Venues

- [ ] **[TC-VEN-001]** `GET /api/venues/{{venueActiveCode}}`: Responde `200 OK` con `VenueResponse` detallando código `VEN-SMR-01`, capacidad 500 y `active=true`.
- [ ] **[TC-VEN-002]** `GET /api/venues/active`: Responde `200 OK`. Ningún venue inactivo (ej. `VEN-INA-99`) figura en la lista.
- [ ] **[TC-VEN-003]** `GET /api/venues/VEN-NONEXISTENT-999`: Responde `404 Not Found` con cuerpo uniforme `ErrorResponse` (`timestamp`, `status: 404`, `error: "Not Found"`, `message`, `details`).

### Módulo 02: Artists

- [ ] **[TC-ART-001]** `GET /api/artists/{{artistId}}`: Responde `200 OK` con `ArtistResponse` (ID 1, `stageName: "Solar Beat"`, `active: true`).
- [ ] **[TC-ART-002]** `GET /api/artists/by-stage-name?stageName={{artistStageName}}`: Responde `200 OK` con los mismos datos del artista `Solar Beat`.
- [ ] **[TC-ART-003]** `GET /api/artists/active`: Responde `200 OK` con todos los artistas activos.
- [ ] **[TC-ART-004]** `GET /api/artists/99999`: Responde `404 Not Found` con `ErrorResponse`.
- [ ] **[TC-ART-005]** `GET /api/artists/by-stage-name?stageName=InexistenteXYZ`: Responde `404 Not Found` con `ErrorResponse`.

### Módulo 03: Auth / Usuarios

- [ ] **[TC-AUTH-001]** `POST /api/users` (USER_A): Responde `201 Created` con `UserResponse` (`id`, `username: "{{userAUsername}}"`, `email: "{{userAEmail}}"`, `active: true`). No expone contraseñas ni hashes.
- [ ] **[TC-AUTH-002]** `POST /api/users` (USER_B): Responde `201 Created` con `UserResponse` para Bernardo.
- [ ] **[TC-AUTH-003]** `POST /api/auth/login` (USER_A): *[BLOQUEADO en Fase 3]* Requiere Fase 4. Esperado: `200 OK` con `accessToken`, `tokenType: "Bearer"`, `expiresIn: 900`.
- [ ] **[TC-AUTH-004]** `POST /api/auth/login` (por Email): *[BLOQUEADO en Fase 3]* Esperado: `200 OK`.
- [ ] **[TC-AUTH-005]** `POST /api/auth/login` (STAFF): *[BLOQUEADO en Fase 3]* Esperado: `200 OK`. Copiar token a `staffToken`.
- [ ] **[TC-AUTH-006]** `POST /api/auth/login` (ADMIN): *[BLOQUEADO en Fase 3]* Esperado: `200 OK`. Copiar token a `adminToken`.
- [ ] **[TC-AUTH-007]** `GET /api/auth/me` (USER_A): *[BLOQUEADO en Fase 3]* Esperado: `200 OK` reflejando la identidad de USER_A sin exponer credenciales.
- [ ] **[TC-AUTH-008]** `GET /api/auth/me` (ADMIN): *[BLOQUEADO en Fase 3]* Esperado: `200 OK` con rol `ADMIN`.
- [ ] **[TC-AUTH-009]** `POST /api/auth/login` (Password incorrecto): *[BLOQUEADO en Fase 3]* Esperado: `401 Unauthorized` genérico.
- [ ] **[TC-AUTH-010]** `POST /api/auth/login` (Identificador inexistente): *[BLOQUEADO en Fase 3]* Esperado: `401 Unauthorized` genérico sin revelar existencia.

### Módulo 04: Usuarios / Consulta y Propiedad

- [ ] **[TC-USR-001]** `GET /api/users/by-email?email={{userAEmail}}`: Responde `200 OK` con `UserResponse` de Andrea. Búsqueda insensible a mayúsculas/minúsculas.
- [ ] **[TC-USR-002]** `GET /api/users/by-username?username={{userAUsername}}`: Responde `200 OK` con `UserResponse` de Andrea.
- [ ] **[TC-USR-003]** `GET /api/users/by-email?email={{userAEmail}}` (como ADMIN): Responde `200 OK`.
- [ ] **[TC-USR-004]** `GET /api/users/by-email?email=inexistente.e2e@example.test`: Responde `404 Not Found` con mensaje `"User not found with email: ..."`.
- [ ] **[TC-USR-005]** `GET /api/users/by-username?username=usuario_fantasma_999`: Responde `404 Not Found` con mensaje `"User not found with username: ..."`.

### Módulo 05: Events / Administración y Visibilidad

- [ ] **[TC-EVT-001]** `POST /api/events` (ADMIN): Responde `201 Created` con `EventResponse` en estado `DRAFT`, `minimumAge: 18`, `venueCode: "{{venueActiveCode}}"`. Guardar `eventCode` en `newEventCode`.
- [ ] **[TC-EVT-002]** `GET /api/events/{{publishedEventCode}}`: Responde `200 OK` con `EventResponse` de `EVT-PUB-01` en estado `PUBLISHED`.
- [ ] **[TC-EVT-003]** `GET /api/events/published`: Responde `200 OK` con lista de eventos publicados ordenados por fecha ascendente. Comprobar que los borradores no aparecen.
- [ ] **[TC-EVT-004]** `PATCH /api/events/{{eventToPublishCode}}/publish`: Responde `200 OK` con status `PUBLISHED`.
- [ ] **[TC-EVT-005]** `POST /api/events/{{newEventCode}}/artists/{{artistId}}`: Responde `200 OK` con el artista incorporado al conjunto `artists`.
- [ ] **[TC-EVT-006]** `GET /api/events/by-artist?stageName={{artistStageName}}`: Responde `200 OK` incluyendo los eventos asociados al artista.
- [ ] **[TC-EVT-007]** `GET /api/events/EVT-NONEXISTENT-999`: Responde `404 Not Found` con `ErrorResponse`.

### Módulo 06: Tickets / Compra, Consulta y Ciclo de Vida

- [ ] **[TC-TKT-001]** `POST /api/tickets` (GENERAL): Responde `201 Created` con `TicketResponse` (`status: "PAID"`, `price: 50.00`, código prefijado con `"TCK-"`). Guardar código en `ticketCancelableCode`.
- [ ] **[TC-TKT-002]** `POST /api/tickets` (VIP): Responde `201 Created` con `TicketResponse` (`status: "PAID"`, `price: 100.00`). Guardar código en `ticketUsableCode`.
- [ ] **[TC-TKT-003]** `GET /api/tickets/{{ticketCancelableCode}}`: Responde `200 OK` con los datos del ticket adquirido.
- [ ] **[TC-TKT-004]** `GET /api/tickets/by-user?email={{userAEmail}}`: Responde `200 OK` con la lista de tickets del usuario Andrea ordenados por fecha descendente.
- [ ] **[TC-TKT-005]** `GET /api/events/{{publishedEventCode}}/tickets/paid`: Responde `200 OK` con la lista operativa de tickets pagados del evento.
- [ ] **[TC-TKT-006]** `PATCH /api/tickets/{{ticketCancelableCode}}/cancel`: Responde `200 OK` con `status: "CANCELLED"`.
- [ ] **[TC-TKT-007]** `GET /api/tickets/{{ticketCancelableCode}}`: Reconsulta posterior que confirma persistencia en base de datos con `status: "CANCELLED"`.
- [ ] **[TC-TKT-008]** `PATCH /api/tickets/{{ticketUsableCode}}/use`: Responde `200 OK` con `status: "USED"`.
- [ ] **[TC-TKT-009]** `GET /api/tickets/{{ticketUsableCode}}`: Reconsulta posterior que confirma persistencia con `status: "USED"`.
- [ ] **[TC-TKT-010]** `GET /api/tickets/TCK-NONEXISTENT-999`: Responde `404 Not Found` con `ErrorResponse`.

### Módulo 07: Seguridad / Escenarios Negativos (401, 403, IDOR)

- [ ] **[TC-SEC-001]** `POST /api/events` sin Authorization: *[BLOQUEADO en Fase 3]* Esperado: `401 Unauthorized`.
- [ ] **[TC-SEC-002]** `POST /api/events` con `userAToken`: *[BLOQUEADO en Fase 3]* Esperado: `403 Forbidden`. Verificar que no se crea el evento.
- [ ] **[TC-SEC-003]** `PATCH /api/events/{{draftEventCode}}/publish` con `userAToken`: *[BLOQUEADO en Fase 3]* Esperado: `403 Forbidden`. Verificar que el evento sigue en `DRAFT`.
- [ ] **[TC-SEC-004]** `PATCH /api/events/{{draftEventCode}}/publish` con `staffToken`: *[BLOQUEADO en Fase 3]* Esperado: `403 Forbidden`. STAFF no tiene permiso de publicación.
- [ ] **[TC-SEC-005]** `POST /api/events/{{draftEventCode}}/artists/{{artistId}}` con `userAToken`: *[BLOQUEADO en Fase 3]* Esperado: `403 Forbidden`.
- [ ] **[TC-SEC-006]** `GET /api/events/{{draftEventCode}}` como anónimo: *[BLOQUEADO en Fase 3]* Esperado: Denegado (`403` o `404` anti-enumeración).
- [ ] **[TC-SEC-007]** `GET /api/users/by-email` de USER_A solicitado por USER_B: *[BLOQUEADO en Fase 3]* Esperado: Denegado (`403` o `404`).
- [ ] **[TC-SEC-008]** `GET /api/users/by-username` de USER_A solicitado por USER_B: *[BLOQUEADO en Fase 3]* Esperado: Denegado (`403` o `404`).
- [ ] **[TC-SEC-009]** `POST /api/tickets` con `userAToken` asignando `userEmail` de USER_B: *[BLOQUEADO en Fase 3]* Esperado: Rechazo (403 o 409). Verificar que no se generó ticket a nombre de B.
- [ ] **[TC-SEC-010]** `GET /api/tickets/{{ticketCancelableCode}}` de USER_A solicitado por USER_B: *[BLOQUEADO en Fase 3]* Esperado: Denegado (`403` o `404` anti-IDOR).
- [ ] **[TC-SEC-011]** `GET /api/tickets/by-user?email={{userAEmail}}` solicitado por USER_B: *[BLOQUEADO en Fase 3]* Esperado: Denegado (`403` o `404`).
- [ ] **[TC-SEC-012]** `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` solicitado por USER_B: *[BLOQUEADO en Fase 3]* Esperado: Denegado (`403` o `404`). Reconsultar con USER_A y verificar que el ticket sigue `PAID` (sin mutación).
- [ ] **[TC-SEC-013]** `PATCH /api/tickets/{{ticketUsableCode}}/use` ejecutado por USER_A: *[BLOQUEADO en Fase 3]* Esperado: `403 Forbidden`. Solo STAFF puede marcar uso.
- [ ] **[TC-SEC-014]** `GET /api/events/{{publishedEventCode}}/tickets/paid` solicitado por USER_A: *[BLOQUEADO en Fase 3]* Esperado: `403 Forbidden`.
- [ ] **[TC-SEC-015]** Token de autorización malformado: *[BLOQUEADO en Fase 3]* Esperado: `401 Unauthorized`.
- [ ] **[TC-SEC-016]** Token JWT con firma alterada: *[BLOQUEADO en Fase 3]* Esperado: `401 Unauthorized`.
- [ ] **[TC-SEC-017]** Token entregado por query param: *[BLOQUEADO en Fase 3]* Esperado: `401 Unauthorized`.
- [ ] **[TC-SEC-018]** Registro intentando inyectar `role="ADMIN"`: Responde `201 Created` (o `400 Bad Request`), pero el usuario nunca adquiere permisos de administrador.

### Módulo 08: Validaciones y Errores HTTP

- [ ] **[TC-HTTP-001]** Registro con cuerpo `{}`: Responde `400 Bad Request` con mapa `details` conteniendo errores en campos obligatorios.
- [ ] **[TC-HTTP-002]** Registro con formato de email inválido: Responde `400 Bad Request` con `details.email`.
- [ ] **[TC-HTTP-003]** Registro con `birthDate` futura: Responde `409 Conflict` con mensaje `"Birth date cannot be in the future"`.
- [ ] **[TC-HTTP-004]** Registro duplicado con mismo `username`: Responde `409 Conflict` con mensaje `"Username already exists: ..."`.
- [ ] **[TC-HTTP-005]** Registro duplicado con mismo `email`: Responde `409 Conflict` con mensaje `"Email already exists: ..."`.
- [ ] **[TC-HTTP-006]** Petición con JSON malformado: Responde `400 Bad Request` con mensaje `"Malformed JSON request"`.
- [ ] **[TC-HTTP-007]** Crear evento con categoría enum inválida: Responde `400 Bad Request`.
- [ ] **[TC-HTTP-008]** Crear evento con fecha pasada: Responde `409 Conflict` con mensaje `"Event date must be in the future"`.
- [ ] **[TC-HTTP-009]** Crear evento con venue inexistente: Responde `404 Not Found` con mensaje `"Venue not found: ..."`.
- [ ] **[TC-HTTP-010]** Crear evento con venue inactivo: Responde `409 Conflict` con mensaje `"Cannot create event in an inactive venue: ..."`.
- [ ] **[TC-HTTP-011]** Crear evento duplicado con mismo `eventCode`: Responde `409 Conflict` con mensaje `"Event code already exists: ..."`.
- [ ] **[TC-HTTP-012]** Publicar evento que ya está en estado `PUBLISHED`: Responde `409 Conflict` con mensaje `"Only DRAFT events can be published. Current status: PUBLISHED"`.
- [ ] **[TC-HTTP-013]** Asociar dos veces el mismo artista: Responde `409 Conflict` con mensaje `"Artist is already associated with this event: ..."`.
- [ ] **[TC-HTTP-014]** Comprar ticket para evento no publicado (`DRAFT`): Responde `409 Conflict` con mensaje `"Tickets can only be purchased for PUBLISHED events"`.
- [ ] **[TC-HTTP-015]** Comprar ticket con tipo enum inválido: Responde `400 Bad Request` con mensaje `"Malformed JSON request"`.
- [ ] **[TC-HTTP-016]** Cancelar un ticket que ya está cancelado: Responde `409 Conflict` con mensaje `"Only PAID tickets can be cancelled. Current ticket status: CANCELLED"`.
- [ ] **[TC-HTTP-017]** Marcar como usado un ticket que ya fue usado: Responde `409 Conflict` con mensaje `"Only PAID tickets can be marked as used. Current ticket status: USED"`.
- [ ] **[TC-HTTP-018]** Intentar marcar como usado un ticket cancelado: Responde `409 Conflict` con mensaje `"Only PAID tickets can be marked as used. Current ticket status: CANCELLED"`.

### Módulo 09: Flujos de Negocio Integrados

- [ ] **Flujo A (Descubrimiento público):** `A.1` Venues activos -> `A.2` Venue por código -> `A.3` Cartelera publicada -> `A.4` Eventos por artista.
- [ ] **Flujo B (Registro e identidades):** `B.1` Registro USER_A -> `B.2` Registro USER_B -> *[B.3-B.8 Logins y /me bloqueados por Fase 4]*.
- [ ] **Flujo C (Gestión de catálogo):** `C.1` Crear evento DRAFT -> `C.2` Asociar artista -> `C.3` Publicar evento -> Reconsultar en cartelera pública.
- [ ] **Flujo D (Compra, propiedad, cancelación y uso):** `D.1` Compra ticket GENERAL -> `D.2` Cancelar ticket -> Reconsultar cancelado -> `D.3` Marcar ticket VIP como usado por STAFF -> Reconsultar usado.
- [ ] **Flujo E (Regresión de errores):** Validación uniforme de los 5 campos de `ErrorResponse` (`timestamp`, `status`, `error`, `message`, `details`) en respuestas 400, 404 y 409.
