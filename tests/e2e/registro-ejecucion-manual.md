# PulsePass — Bitácora de Registro de Ejecución Manual E2E

Esta bitácora permite registrar los resultados obtenidos durante la ejecución manual de la colección Postman `PulsePass — E2E Manual`. Cada solicitud se envía individualmente mediante el botón `Send` de Postman, inspeccionando visualmente el código de estado HTTP, los encabezados, la estructura del cuerpo JSON y los efectos de negocio persistidos en la base de datos `pulsepass_e2e`.

---

## 1. Convenciones y Estados de Ejecución

- **`PASS`**: El código HTTP, la estructura JSON y las condiciones de negocio/seguridad observadas coinciden exactamente con lo esperado.
- **`FAIL`**: Cualquiera de las comprobaciones no coincide, o se produjo un error 500 no previsto.
- **`BLOCKED`**: La prueba no puede ejecutarse debido a una precondición ausente (fixture faltante, dependencia técnica no implementada en la versión del backend).
- **`NOT RUN`**: Caso de prueba pendiente de ser ejecutado por el evaluador.

> [!IMPORTANT]
> **REGLA DE INTEGRIDAD ACADÉMICA Y TÉCNICA:**
> No se inventan resultados ni se declaran aprobaciones sin evidencia.
> En la versión actual del repositorio, la **Capa de Controladores REST (Fase 3)** se encuentra implementada (20 rutas de negocio disponibles para ejecución manual).
> La **Capa de Seguridad Spring Security + JJWT y AuthController (Fase 4)** aún no ha sido integrada en este repositorio; por consiguiente, los casos que dependen de `/api/auth/login`, `/api/auth/me` o de la intercepción de roles por filtros HTTP de seguridad se registran estrictamente como **`BLOCKED`** debido a esta dependencia bloqueante.

---

## 2. Resumen Consolidado de Ejecución

| Módulo / Categoría | Total Casos | PASS | FAIL | BLOCKED | NOT RUN |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **00 Conectividad y Preparación** | 2 | 0 | 0 | 0 | 2 |
| **01 Venues** | 3 | 0 | 0 | 0 | 3 |
| **02 Artists** | 5 | 0 | 0 | 0 | 5 |
| **03 Auth / Usuarios** | 10 | 0 | 0 | 8 | 2 |
| **04 Usuarios / Consulta y Propiedad** | 5 | 0 | 0 | 0 | 5 |
| **05 Events / Catálogo** | 7 | 0 | 0 | 0 | 7 |
| **06 Tickets / Ciclo de Vida** | 10 | 0 | 0 | 0 | 10 |
| **07 Seguridad / Escenarios Negativos** | 18 | 0 | 0 | 17 | 1 |
| **08 Validaciones y Errores HTTP** | 18 | 0 | 0 | 0 | 18 |
| **09 Flujos de Negocio Integrados** | 12 | 0 | 0 | 0 | 12 |
| **TOTAL GENERAL** | **90** | **0** | **0** | **25** | **65** |

---

## 3. Registro Detallado por Caso de Prueba

### Módulo 00: Preparación / Conectividad

| Caso (ID) | Fecha / Hora | Actor | Método y Ruta | HTTP Esperado | HTTP Observado | JSON / Estado Observado | Resultado | Evidencia / Observaciones |
| :--- | :---: | :---: | :--- | :---: | :---: | :---: | :---: | :--- |
| `TC-CONN-001` | *Pendiente* | `ANONYMOUS` | `GET /api/venues/active` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Comprobar disponibilidad de API y venue `VEN-SMR-01`. |
| `TC-CONN-002` | *Pendiente* | `ANONYMOUS` | `GET /api/artists/active` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Comprobar catálogo inicial Flyway V2. |

### Módulo 01: Venues

| Caso (ID) | Fecha / Hora | Actor | Método y Ruta | HTTP Esperado | HTTP Observado | JSON / Estado Observado | Resultado | Evidencia / Observaciones |
| :--- | :---: | :---: | :--- | :---: | :---: | :---: | :---: | :--- |
| `TC-VEN-001` | *Pendiente* | `ANONYMOUS` | `GET /api/venues/{{venueActiveCode}}` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Validar `VenueResponse` con capacidad 500. |
| `TC-VEN-002` | *Pendiente* | `ANONYMOUS` | `GET /api/venues/active` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Excluye venues con `active=false`. |
| `TC-VEN-003` | *Pendiente* | `ANONYMOUS` | `GET /api/venues/VEN-NONEXISTENT-999` | `404` | *pendiente* | *pendiente* | `NOT RUN` | Validar estructura `ErrorResponse` uniforme. |

### Módulo 02: Artists

| Caso (ID) | Fecha / Hora | Actor | Método y Ruta | HTTP Esperado | HTTP Observado | JSON / Estado Observado | Resultado | Evidencia / Observaciones |
| :--- | :---: | :---: | :--- | :---: | :---: | :---: | :---: | :--- |
| `TC-ART-001` | *Pendiente* | `ANONYMOUS` | `GET /api/artists/{{artistId}}` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Verificar artista `Solar Beat` (ID 1). |
| `TC-ART-002` | *Pendiente* | `ANONYMOUS` | `GET /api/artists/by-stage-name?stageName=...` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Búsqueda por query parameter. |
| `TC-ART-003` | *Pendiente* | `ANONYMOUS` | `GET /api/artists/active` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Verificar que solo lista activos. |
| `TC-ART-004` | *Pendiente* | `ANONYMOUS` | `GET /api/artists/99999` | `404` | *pendiente* | *pendiente* | `NOT RUN` | `ResourceNotFoundException`. |
| `TC-ART-005` | *Pendiente* | `ANONYMOUS` | `GET /api/artists/by-stage-name?stageName=InexistenteXYZ` | `404` | *pendiente* | *pendiente* | `NOT RUN` | Búsqueda no encontrada. |

### Módulo 03: Auth / Usuarios

| Caso (ID) | Fecha / Hora | Actor | Método y Ruta | HTTP Esperado | HTTP Observado | JSON / Estado Observado | Resultado | Evidencia / Observaciones |
| :--- | :---: | :---: | :--- | :---: | :---: | :---: | :---: | :--- |
| `TC-AUTH-001` | *Pendiente* | `ANONYMOUS` | `POST /api/users` | `201` | *pendiente* | *pendiente* | `NOT RUN` | Registro de Andrea (USER_A). Implementado en Fase 3. |
| `TC-AUTH-002` | *Pendiente* | `ANONYMOUS` | `POST /api/users` | `201` | *pendiente* | *pendiente* | `NOT RUN` | Registro de Bernardo (USER_B). Implementado en Fase 3. |
| `TC-AUTH-003` | *Pendiente* | `ANONYMOUS` | `POST /api/auth/login` | `200` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Endpoint no implementado en repo actual. |
| `TC-AUTH-004` | *Pendiente* | `ANONYMOUS` | `POST /api/auth/login` | `200` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Endpoint no implementado en repo actual. |
| `TC-AUTH-005` | *Pendiente* | `ANONYMOUS` | `POST /api/auth/login` | `200` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Endpoint no implementado en repo actual. |
| `TC-AUTH-006` | *Pendiente* | `ANONYMOUS` | `POST /api/auth/login` | `200` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Endpoint no implementado en repo actual. |
| `TC-AUTH-007` | *Pendiente* | `USER_A` | `GET /api/auth/me` | `200` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Endpoint no implementado en repo actual. |
| `TC-AUTH-008` | *Pendiente* | `ADMIN` | `GET /api/auth/me` | `200` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Endpoint no implementado en repo actual. |
| `TC-AUTH-009` | *Pendiente* | `ANONYMOUS` | `POST /api/auth/login` | `401` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Endpoint no implementado en repo actual. |
| `TC-AUTH-010` | *Pendiente* | `ANONYMOUS` | `POST /api/auth/login` | `401` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Endpoint no implementado en repo actual. |

### Módulo 04: Usuarios / Consulta y Propiedad

| Caso (ID) | Fecha / Hora | Actor | Método y Ruta | HTTP Esperado | HTTP Observado | JSON / Estado Observado | Resultado | Evidencia / Observaciones |
| :--- | :---: | :---: | :--- | :---: | :---: | :---: | :---: | :--- |
| `TC-USR-001` | *Pendiente* | `USER_A` | `GET /api/users/by-email?email=...` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Consulta insensible a mayúsculas/minúsculas. |
| `TC-USR-002` | *Pendiente* | `USER_A` | `GET /api/users/by-username?username=...` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Consulta por username. |
| `TC-USR-003` | *Pendiente* | `ADMIN` | `GET /api/users/by-email?email=...` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Consulta administrativa de usuario. |
| `TC-USR-004` | *Pendiente* | `ANONYMOUS` | `GET /api/users/by-email?email=inexistente...` | `404` | *pendiente* | *pendiente* | `NOT RUN` | `ResourceNotFoundException`. |
| `TC-USR-005` | *Pendiente* | `ANONYMOUS` | `GET /api/users/by-username?username=fantasma...` | `404` | *pendiente* | *pendiente* | `NOT RUN` | `ResourceNotFoundException`. |

### Módulo 05: Events / Catálogo y Visibilidad

| Caso (ID) | Fecha / Hora | Actor | Método y Ruta | HTTP Esperado | HTTP Observado | JSON / Estado Observado | Resultado | Evidencia / Observaciones |
| :--- | :---: | :---: | :--- | :---: | :---: | :---: | :---: | :--- |
| `TC-EVT-001` | *Pendiente* | `ADMIN` | `POST /api/events` | `201` | *pendiente* | *pendiente* | `NOT RUN` | Evento creado con status inicial `DRAFT`. |
| `TC-EVT-002` | *Pendiente* | `ANONYMOUS` | `GET /api/events/{{publishedEventCode}}` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Consultar evento publicado por código. |
| `TC-EVT-003` | *Pendiente* | `ANONYMOUS` | `GET /api/events/published` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Cartelera pública excluye borradores. |
| `TC-EVT-004` | *Pendiente* | `ADMIN` | `PATCH /api/events/{{eventToPublishCode}}/publish` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Transición a `PUBLISHED`. |
| `TC-EVT-005` | *Pendiente* | `ADMIN` | `POST /api/events/{{newEventCode}}/artists/{{artistId}}` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Asociar artista al evento. |
| `TC-EVT-006` | *Pendiente* | `ANONYMOUS` | `GET /api/events/by-artist?stageName=...` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Eventos por artista invitado. |
| `TC-EVT-007` | *Pendiente* | `ANONYMOUS` | `GET /api/events/EVT-NONEXISTENT-999` | `404` | *pendiente* | *pendiente* | `NOT RUN` | Evento inexistente. |

### Módulo 06: Tickets / Compra y Ciclo de Vida

| Caso (ID) | Fecha / Hora | Actor | Método y Ruta | HTTP Esperado | HTTP Observado | JSON / Estado Observado | Resultado | Evidencia / Observaciones |
| :--- | :---: | :---: | :--- | :---: | :---: | :---: | :---: | :--- |
| `TC-TKT-001` | *Pendiente* | `USER_A` | `POST /api/tickets` | `201` | *pendiente* | *pendiente* | `NOT RUN` | Compra ticket GENERAL ($50.00). Guardar `ticketCancelableCode`. |
| `TC-TKT-002` | *Pendiente* | `USER_A` | `POST /api/tickets` | `201` | *pendiente* | *pendiente* | `NOT RUN` | Compra ticket VIP ($100.00). Guardar `ticketUsableCode`. |
| `TC-TKT-003` | *Pendiente* | `USER_A` | `GET /api/tickets/{{ticketCancelableCode}}` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Consulta ticket propio por código. |
| `TC-TKT-004` | *Pendiente* | `USER_A` | `GET /api/tickets/by-user?email=...` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Historial de compras del usuario. |
| `TC-TKT-005` | *Pendiente* | `STAFF` | `GET /api/events/{{publishedEventCode}}/tickets/paid` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Lista operativa de tickets pagados. |
| `TC-TKT-006` | *Pendiente* | `USER_A` | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Transición a `CANCELLED`. |
| `TC-TKT-007` | *Pendiente* | `USER_A` | `GET /api/tickets/{{ticketCancelableCode}}` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Reconsulta posterior: confirma persistencia `CANCELLED`. |
| `TC-TKT-008` | *Pendiente* | `STAFF` | `PATCH /api/tickets/{{ticketUsableCode}}/use` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Transición a `USED` por STAFF. |
| `TC-TKT-009` | *Pendiente* | `USER_A` | `GET /api/tickets/{{ticketUsableCode}}` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Reconsulta posterior: confirma persistencia `USED`. |
| `TC-TKT-010` | *Pendiente* | `ANONYMOUS` | `GET /api/tickets/TCK-NONEXISTENT-999` | `404` | *pendiente* | *pendiente* | `NOT RUN` | Ticket no encontrado. |

### Módulo 07: Seguridad / Escenarios Negativos (401, 403, IDOR)

| Caso (ID) | Fecha / Hora | Actor | Método y Ruta | HTTP Esperado | HTTP Observado | JSON / Estado Observado | Resultado | Evidencia / Observaciones |
| :--- | :---: | :---: | :--- | :---: | :---: | :---: | :---: | :--- |
| `TC-SEC-001` | *Pendiente* | `ANONYMOUS` | `POST /api/events` | `401` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Sin capa de seguridad activa. |
| `TC-SEC-002` | *Pendiente* | `USER_A` | `POST /api/events` | `403` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Sin verificación de rol ADMIN. |
| `TC-SEC-003` | *Pendiente* | `USER_A` | `PATCH /api/events/.../publish` | `403` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Sin verificación de rol ADMIN. |
| `TC-SEC-004` | *Pendiente* | `STAFF` | `PATCH /api/events/.../publish` | `403` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Sin restricción de rol STAFF. |
| `TC-SEC-005` | *Pendiente* | `USER_A` | `POST /api/events/.../artists/...` | `403` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Sin verificación de rol ADMIN. |
| `TC-SEC-006` | *Pendiente* | `ANONYMOUS` | `GET /api/events/{{draftEventCode}}` | `403/404` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Sin ocultamiento de borrador en API pública. |
| `TC-SEC-007` | *Pendiente* | `USER_B` | `GET /api/users/by-email` (de A) | `403/404` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Sin validación de ownership. |
| `TC-SEC-008` | *Pendiente* | `USER_B` | `GET /api/users/by-username` (de A) | `403/404` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Sin validación de ownership. |
| `TC-SEC-009` | *Pendiente* | `USER_A` | `POST /api/tickets` (email de B) | `403/409` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Sin verificación de principal vs body. |
| `TC-SEC-010` | *Pendiente* | `USER_B` | `GET /api/tickets/{{ticketCancelableCode}}` | `403/404` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Sin validación de ownership en tickets. |
| `TC-SEC-011` | *Pendiente* | `USER_B` | `GET /api/tickets/by-user` (de A) | `403/404` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Sin validación de ownership en listado. |
| `TC-SEC-012` | *Pendiente* | `USER_B` | `PATCH /api/tickets/.../cancel` (de A) | `403/404` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Sin validación de ownership en cancelación. |
| `TC-SEC-013` | *Pendiente* | `USER_A` | `PATCH /api/tickets/.../use` | `403` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Sin restricción de rol STAFF. |
| `TC-SEC-014` | *Pendiente* | `USER_A` | `GET /api/events/.../tickets/paid` | `403` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Sin restricción de rol STAFF/ADMIN. |
| `TC-SEC-015` | *Pendiente* | `ANONYMOUS` | `GET /api/auth/me` (Token malformado) | `401` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Sin filtro JJWT. |
| `TC-SEC-016` | *Pendiente* | `ANONYMOUS` | `GET /api/auth/me` (Firma alterada) | `401` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Sin filtro JJWT. |
| `TC-SEC-017` | *Pendiente* | `ANONYMOUS` | `GET /api/auth/me?token=...` | `401` | N/A | N/A | `BLOCKED` | **Dependencia Fase 4:** Sin filtro JJWT. |
| `TC-SEC-018` | *Pendiente* | `ANONYMOUS` | `POST /api/users` (inyección role) | `201` | *pendiente* | *pendiente* | `NOT RUN` | Validar que no se conceden privilegios ADMIN. |

### Módulo 08: Validaciones y Errores HTTP

| Caso (ID) | Fecha / Hora | Actor | Método y Ruta | HTTP Esperado | HTTP Observado | JSON / Estado Observado | Resultado | Evidencia / Observaciones |
| :--- | :---: | :---: | :--- | :---: | :---: | :---: | :---: | :--- |
| `TC-HTTP-001` | *Pendiente* | `ANONYMOUS` | `POST /api/users` `{}` | `400` | *pendiente* | *pendiente* | `NOT RUN` | Validación de campos obligatorios (`details`). |
| `TC-HTTP-002` | *Pendiente* | `ANONYMOUS` | `POST /api/users` (bad email) | `400` | *pendiente* | *pendiente* | `NOT RUN` | Validación `@Email`. |
| `TC-HTTP-003` | *Pendiente* | `ANONYMOUS` | `POST /api/users` (birthDate futura) | `409` | *pendiente* | *pendiente* | `NOT RUN` | Regla `BR-USER-005`. |
| `TC-HTTP-004` | *Pendiente* | `ANONYMOUS` | `POST /api/users` (username dup) | `409` | *pendiente* | *pendiente* | `NOT RUN` | Regla `BR-USER-001`. |
| `TC-HTTP-005` | *Pendiente* | `ANONYMOUS` | `POST /api/users` (email dup) | `409` | *pendiente* | *pendiente* | `NOT RUN` | Regla `BR-USER-002`. |
| `TC-HTTP-006` | *Pendiente* | `ANONYMOUS` | `POST /api/events` (JSON roto) | `400` | *pendiente* | *pendiente* | `NOT RUN` | `HttpMessageNotReadableException`. |
| `TC-HTTP-007` | *Pendiente* | `ADMIN` | `POST /api/events` (bad category) | `400` | *pendiente* | *pendiente* | `NOT RUN` | Deserialización de enum `EventCategory`. |
| `TC-HTTP-008` | *Pendiente* | `ADMIN` | `POST /api/events` (fecha pasada) | `409` | *pendiente* | *pendiente* | `NOT RUN` | Regla `BR-EVENT-004`. |
| `TC-HTTP-009` | *Pendiente* | `ADMIN` | `POST /api/events` (venue inex.) | `404` | *pendiente* | *pendiente* | `NOT RUN` | Regla `BR-EVENT-002`. |
| `TC-HTTP-010` | *Pendiente* | `ADMIN` | `POST /api/events` (venue inactivo) | `409` | *pendiente* | *pendiente* | `NOT RUN` | Regla `BR-EVENT-003`. |
| `TC-HTTP-011` | *Pendiente* | `ADMIN` | `POST /api/events` (eventCode dup) | `409` | *pendiente* | *pendiente* | `NOT RUN` | Regla `BR-EVENT-001`. |
| `TC-HTTP-012` | *Pendiente* | `ADMIN` | `PATCH /api/events/.../publish` (no DRAFT) | `409` | *pendiente* | *pendiente* | `NOT RUN` | Regla `BR-EVENT-007`. |
| `TC-HTTP-013` | *Pendiente* | `ADMIN` | `POST /api/events/.../artists/...` (dup) | `409` | *pendiente* | *pendiente* | `NOT RUN` | Regla `BR-EVENT-010`. |
| `TC-HTTP-014` | *Pendiente* | `USER_A` | `POST /api/tickets` (evento DRAFT) | `409` | *pendiente* | *pendiente* | `NOT RUN` | Regla `BR-TICKET-004`. |
| `TC-HTTP-015` | *Pendiente* | `USER_A` | `POST /api/tickets` (bad type) | `400` | *pendiente* | *pendiente* | `NOT RUN` | Deserialización enum `TicketType`. |
| `TC-HTTP-016` | *Pendiente* | `USER_A` | `PATCH /api/tickets/.../cancel` (ya cancelado) | `409` | *pendiente* | *pendiente* | `NOT RUN` | Regla `BR-TICKET-010`. |
| `TC-HTTP-017` | *Pendiente* | `STAFF` | `PATCH /api/tickets/.../use` (ya usado) | `409` | *pendiente* | *pendiente* | `NOT RUN` | Regla `BR-TICKET-013`. |
| `TC-HTTP-018` | *Pendiente* | `STAFF` | `PATCH /api/tickets/.../use` (cancelado) | `409` | *pendiente* | *pendiente* | `NOT RUN` | Regla `BR-TICKET-013`. |

### Módulo 09: Flujos de Negocio Integrados

| Caso (ID) | Fecha / Hora | Actor | Método y Ruta | HTTP Esperado | HTTP Observado | JSON / Estado Observado | Resultado | Evidencia / Observaciones |
| :--- | :---: | :---: | :--- | :---: | :---: | :---: | :---: | :--- |
| `TC-BIZ-001` | *Pendiente* | `ANONYMOUS` | `GET /api/venues/active` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Flujo A.1 - Descubrimiento de recintos. |
| `TC-BIZ-002` | *Pendiente* | `ANONYMOUS` | `GET /api/venues/{{venueActiveCode}}` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Flujo A.2 - Verificación de recinto. |
| `TC-BIZ-003` | *Pendiente* | `ANONYMOUS` | `GET /api/events/published` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Flujo A.3 - Cartelera pública. |
| `TC-BIZ-004` | *Pendiente* | `ANONYMOUS` | `GET /api/events/by-artist?stageName=...` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Flujo A.4 - Eventos de artista. |
| `TC-BIZ-005` | *Pendiente* | `ANONYMOUS` | `POST /api/users` (USER_A) | `201` | *pendiente* | *pendiente* | `NOT RUN` | Flujo B.1 - Registro cliente. |
| `TC-BIZ-006` | *Pendiente* | `ANONYMOUS` | `POST /api/users` (USER_B) | `201` | *pendiente* | *pendiente* | `NOT RUN` | Flujo B.2 - Registro segundo usuario. |
| `TC-BIZ-007` | *Pendiente* | `ADMIN` | `POST /api/events` | `201` | *pendiente* | *pendiente* | `NOT RUN` | Flujo C.1 - Creación evento borrador. |
| `TC-BIZ-008` | *Pendiente* | `ADMIN` | `POST /api/events/.../artists/...` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Flujo C.2 - Vinculación de artista. |
| `TC-BIZ-009` | *Pendiente* | `ADMIN` | `PATCH /api/events/.../publish` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Flujo C.3 - Publicación de evento. |
| `TC-BIZ-010` | *Pendiente* | `USER_A` | `POST /api/tickets` | `201` | *pendiente* | *pendiente* | `NOT RUN` | Flujo D.1 - Compra entrada. |
| `TC-BIZ-011` | *Pendiente* | `USER_A` | `PATCH /api/tickets/.../cancel` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Flujo D.2 - Cancelación de entrada propia. |
| `TC-BIZ-012` | *Pendiente* | `STAFF` | `PATCH /api/tickets/.../use` | `200` | *pendiente* | *pendiente* | `NOT RUN` | Flujo D.3 - Marcado de uso por STAFF. |
