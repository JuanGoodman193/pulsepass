# PulsePass — Guía de Pruebas End-to-End (E2E) Manuales

Bienvenido a la suite oficial de pruebas end-to-end (E2E) manuales de **PulsePass (Fase 5)**. Este documento proporciona las directrices paso a paso para configurar el entorno de ejecución, aprovisionar datos deterministas, importar la colección y el entorno en Postman, ejecutar manualmente cada solicitud HTTP e inspeccionar los resultados de negocio y seguridad.

---

## 1. Objetivo y Alcance

El objetivo de esta Fase 5 es validar de extremo a extremo las capacidades expuestas por la API REST de PulsePass, contrastando las respuestas HTTP, la persistencia en base de datos PostgreSQL real y las restricciones de acceso definidas en el diseño del producto.

### Alcance Funcional
- **20 Operaciones de Negocio:** Rutas heredadas de las Fases 1 a 3 (Venues, Artistas, Eventos, Usuarios y Tickets).
- **2 Rutas de Autenticación:** Especificadas en la Fase 4 (`POST /api/auth/login` y `GET /api/auth/me`).
- **Verificación de Integridad:** Control de unicidad, máquina de estados finitos (transiciones de eventos y tickets) y cálculo de precios.
- **Validación de Errores y Seguridad:** Inspección visual de contratos de error (`ErrorResponse`), validación de parámetros, deserialización de enums y prevención de accesos cruzados no autorizados (IDOR).

---

## 2. Arquitectura de Ejecución E2E

Las pruebas se ejecutan contra una instancia real y desplegada del backend sin la intermediación de mocks ni simuladores en memoria:

```
+-----------------------------------------------------------------------------------+
|                                  CLIENTE POSTMAN                                  |
|         Envío manual individual de cada solicitud HTTP mediante botón 'Send'       |
+-----------------------------------------------------------------------------------+
                                         │
                                         ▼  HTTP :8080
+-----------------------------------------------------------------------------------+
|                                SERVIDOR SPRING BOOT                               |
|                                                                                   |
|  ┌─────────────────────────────────────────────────────────────────────────────┐  |
|  │ Capa de Seguridad (Spring Security 7.x + JJWT 0.13.x)                      │  |
|  │ - Filtro Bearer JWT, validación de firmas, roles y políticas de propiedad  │  |
|  └──────────────────────────────────────┬──────────────────────────────────────┘  |
|                                         │                                         |
|  ┌──────────────────────────────────────▼──────────────────────────────────────┐  |
|  │ Controladores REST (@RestController) & Manejo Global de Excepciones         │  |
|  │ - Mapeo de 22 rutas, validación @Valid de DTOs, ErrorResponse uniforme     │  |
|  └──────────────────────────────────────┬──────────────────────────────────────┘  |
|                                         │                                         |
|  ┌──────────────────────────────────────▼──────────────────────────────────────┐  |
|  │ Capa de Servicios & Lógica de Negocio                                       │  |
|  │ - Reglas de capacidad, edad mínima, precios y transiciones de estado       │  |
|  └──────────────────────────────────────┬──────────────────────────────────────┘  |
|                                         │                                         |
|  ┌──────────────────────────────────────▼──────────────────────────────────────┐  |
|  │ Repositorios Spring Data JPA & Entidades de Dominio                         │  |
|  └──────────────────────────────────────┬──────────────────────────────────────┘  |
+-----------------------------------------┼-----------------------------------------+
                                          │  JDBC :5432
                                          ▼
+-----------------------------------------------------------------------------------+
|                        POSTGRESQL (pulsepass_e2e DEDICADA)                        |
|   - Migraciones Flyway (V1, V2, V3)                                              |
|   - Fixtures deterministas de prueba (seed-e2e.sql)                               |
+-----------------------------------------------------------------------------------+
```

---

## 3. Estado del Repositorio y Dependencias Bloqueantes

> [!IMPORTANT]
> **REGLA DE PRECEDENCIA Y SITUACIÓN ACTUAL DEL CÓDIGO:**
> 1. En la rama actual, la **Fase 3 (Controladores REST)** se encuentra plenamente implementada, compilada y probada, cubriendo las **20 operaciones de negocio**.
> 2. La **Fase 4 (Capa de Seguridad Spring Security + JJWT y AuthController)** aún no ha sido fusionada en este repositorio. Por consiguiente:
>    - Las rutas `/api/auth/login` y `/api/auth/me` no están presentes en el backend actual.
>    - Los filtros de seguridad para códigos `401 Unauthorized` y `403 Forbidden` no interceptarán las solicitudes hasta que se integre la Fase 4.
> 3. Conforme a las instrucciones del PRD, estas rutas y escenarios de seguridad **están incluidos en la colección Postman** para garantizar la cobertura contractual completa de 22 rutas, pero su ejecución se documenta estrictamente como **`BLOCKED`** (dependencia bloqueante) en la bitácora, sin inventar controladores ni alterar la API para falsear resultados.

---

## 4. Requisitos Previos

- **Java Development Kit (JDK):** Versión 21 LTS (64 bits).
- **Maven:** Versión 3.9 o superior.
- **PostgreSQL:** Versión 16 o superior en ejecución local en el puerto `5432`.
- **Postman:** Postman Desktop (recomendado) o Postman Web Agent v11+.

---

## 5. Preparación del Entorno de Pruebas

### 5.1 Crear la Base de Datos Dedicada

> [!CAUTION]
> Para garantizar el aislamiento y la protección de datos, utilice siempre la base de datos exclusiva `pulsepass_e2e`. Nunca ejecute pruebas contra entornos de producción o desarrollo compartido.

```sql
-- Ejecutar en PostgreSQL local (psql o cliente SQL):
CREATE DATABASE pulsepass_e2e;
```

### 5.2 Aplicar Migraciones Flyway y Semilla de Datos

1. Al iniciar la aplicación PulsePass configurada hacia `pulsepass_e2e`, Flyway ejecutará automáticamente las migraciones:
   - `V1__create_schema.sql` (esquema relacional e índices).
   - `V2__insert_initial_artists.sql` (5 artistas iniciales).
   - `V3__add_streaming_url_to_event.sql` (columna de transmisión).
2. Ejecutar el script de semilla E2E para insertar venues, eventos, usuarios y tickets deterministas:
   ```bash
   psql -h localhost -p 5432 -U postgres -d pulsepass_e2e -f tests/e2e/fixtures/seed-e2e.sql
   ```
   *(Consulte [`tests/e2e/fixtures/README.md`](file:///c:/Users/Juan%20Diego/Desktop/pulsepass/tests/e2e/fixtures/README.md) para más detalles sobre los fixtures aprovisionados).*

### 5.3 Iniciar la Aplicación PulsePass

Inicie la aplicación apuntando a la base de datos de pruebas mediante variables de entorno:

```bash
# En PowerShell:
$env:DB_URL="jdbc:postgresql://localhost:5432/pulsepass_e2e"
$env:DB_USER="postgres"
$env:DB_PASSWORD="tu_password_local"

# Ejecutar con Maven:
mvn spring-boot:run
```

Verifique que en los logs figure `Started PulsePassApplication` en el puerto `8080`.

---

## 6. Configuración de Postman

### 6.1 Importar Entregables

1. Abra **Postman**.
2. Haga clic en **Import** (esquina superior izquierda).
3. Seleccione los dos archivos ubicados en `tests/e2e/postman/`:
   - [`PulsePass-Manual-E2E.postman_collection.json`](file:///c:/Users/Juan%20Diego/Desktop/pulsepass/tests/e2e/postman/PulsePass-Manual-E2E.postman_collection.json)
   - [`PulsePass-local.postman_environment.json`](file:///c:/Users/Juan%20Diego/Desktop/pulsepass/tests/e2e/postman/PulsePass-local.postman_environment.json)
4. En el selector de entornos (esquina superior derecha de Postman), elija **`PulsePass — Local E2E Environment`**.

### 6.2 Gestión Segura de Variables y Tokens

> [!WARNING]
> **REGLA DE CONFIDENCIALIDAD:**
> Por directriz de seguridad, los archivos versionados en Git no contienen contraseñas reales ni tokens JWT. Configure las credenciales locales directamente en la columna **Current Value** de Postman:

- `baseUrl`: `http://localhost:8080`
- `runId`: Identificador único para su sesión (ej. `RUN-001`).
- `userAEmail`: `andrea.pruebas@example.test`
- `userAUsername`: `andrea_e2e`
- `userAPassword`: Su contraseña de prueba local (no se versiona).
- `userAToken`, `userBToken`, `staffToken`, `adminToken`: Almacenan los JWT obtenidos tras el login. Se copian manualmente tras cada respuesta.

---

## 7. Procedimiento Operativo de Ejecución Manual

> [!CAUTION]
> **PROHIBICIÓN ESTRICTA DE AUTOMATIZACIÓN:**
> Está terminantemente prohibido utilizar Collection Runner, Newman, Postman CLI o scripts automatizados en las pestañas *Pre-request* o *Tests*. Cada petición debe enviarse individualmente pulsando el botón **Send**.

### Flujo de Ejecución Recomendado (Capítulo 9 del PRD)

Siga las carpetas en el orden secuencial documentado:

#### Flujo A — Descubrimiento Público (Sin Autenticación)
1. Ejecutar `[TC-CONN-001]` y `[TC-CONN-002]` para verificar disponibilidad del servicio.
2. Ejecutar `[TC-VEN-001]` y `[TC-VEN-002]` para comprobar recintos activos.
3. Ejecutar `[TC-ART-001]`, `[TC-ART-002]` y `[TC-ART-003]` para consultar artistas.
4. Ejecutar `[TC-EVT-003]` y comprobar que los eventos en borrador (`DRAFT`) **no** figuran en la cartelera pública.
5. Ejecutar `[TC-EVT-006]` para consultar eventos asociados al artista `Solar Beat`.

#### Flujo B — Registro y Ciclo de Identidades
1. Ejecutar `[TC-AUTH-001]` (`POST /api/users`) para registrar a `USER_A` (`andrea_e2e`).
2. Ejecutar `[TC-AUTH-002]` (`POST /api/users`) para registrar a `USER_B` (`bernardo_e2e`).
3. *(Una vez integrada la Fase 4)* Ejecutar los logins `TC-AUTH-003` a `TC-AUTH-006`, copiar el valor del campo `accessToken` a las variables locales (`userAToken`, `userBToken`, `staffToken`, `adminToken`) y verificar la identidad con `/api/auth/me`.

#### Flujo C — Gestión del Catálogo (ADMIN)
1. Ejecutar `[TC-EVT-001]` (`POST /api/events`) para crear un evento en estado inicial `DRAFT`.
2. Copiar el `eventCode` retornado a la variable `newEventCode`.
3. Ejecutar `[TC-EVT-005]` (`POST /api/events/.../artists/...`) para asociar al artista `Solar Beat`.
4. Ejecutar `[TC-EVT-004]` (`PATCH /api/events/.../publish`) para transicionar el evento a `PUBLISHED`.
5. Ejecutar `[TC-EVT-003]` para comprobar que ahora sí aparece en el listado público.

#### Flujo D — Compra, Propiedad, Cancelación y Uso de Tickets
1. Ejecutar `[TC-TKT-001]` (`POST /api/tickets`) para comprar un ticket `GENERAL` para `USER_A`. Copiar el código retornado a `ticketCancelableCode`.
2. Ejecutar `[TC-TKT-002]` (`POST /api/tickets`) para comprar un ticket `VIP` para `USER_A`. Copiar el código retornado a `ticketUsableCode`.
3. Ejecutar `[TC-TKT-006]` (`PATCH /api/tickets/.../cancel`) para cancelar el ticket propio. Reconsultar con `[TC-TKT-007]` para comprobar que el estado persistió como `CANCELLED`.
4. Ejecutar `[TC-TKT-008]` (`PATCH /api/tickets/.../use`) con el actor STAFF para marcar el ticket `ticketUsableCode` como `USED`. Reconsultar con `[TC-TKT-009]`.

#### Flujo E — Regresión de Errores y Validaciones
1. Ejecutar las solicitudes de la carpeta `08 Validaciones y errores HTTP` comprobando respuestas `400 Bad Request`, `404 Not Found` y `409 Conflict`.
2. Inspeccionar la presencia estricta de los 5 campos de `ErrorResponse`:
   ```json
   {
     "timestamp": "2026-10-18T19:30:00.000",
     "status": 409,
     "error": "Conflict",
     "message": "Only DRAFT events can be published. Current status: PUBLISHED",
     "details": {}
   }
   ```
3. Verificar que ninguna respuesta de error filtre trazas internas de la pila (stack traces), sentencias SQL, contraseñas o tokens.

---

## 8. Criterios de Aceptación Visual de Respuestas

Para cada solicitud enviada, el evaluador debe verificar:

1. **Status Code:** Coincidencia exacta con el esperado (`200 OK`, `201 Created`, `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `409 Conflict`). Un 2xx genérico no equivale a aprobación.
2. **Encabezado Content-Type:** Debe ser `application/json;charset=UTF-8` en todas las respuestas con cuerpo.
3. **Estructura del Cuerpo:** Coincidencia con los DTOs definidos en el código fuente:
   - `VenueResponse`: `id`, `code`, `name`, `city`, `capacity`, `active`.
   - `ArtistResponse`: `id`, `stageName`, `active`.
   - `EventResponse`: `id`, `eventCode`, `name`, `description`, `category`, `status`, `eventDate`, `minimumAge`, `venueCode`, `venueName`, `artists`.
   - `UserResponse`: `id`, `username`, `email`, `active`, `firstName`, `lastName`, `phone`, `city`, `birthDate`.
   - `TicketResponse`: `id`, `ticketCode`, `type`, `price`, `status`, `purchaseDate`, `userEmail`, `eventCode`, `eventName`.
   - `ErrorResponse`: `timestamp`, `status`, `error`, `message`, `details`.
4. **Verificación de Persistencia:** En operaciones de mutación (`POST`, `PATCH`), verificar los cambios observables mediante un `GET` posterior.

---

## 9. Registro de Evidencias

- Complete manualmente cada fila en [`tests/e2e/registro-ejecucion-manual.md`](file:///c:/Users/Juan%20Diego/Desktop/pulsepass/tests/e2e/registro-ejecucion-manual.md).
- Utilice el checklist en [`tests/e2e/checklist-e2e-manual.md`](file:///c:/Users/Juan%20Diego/Desktop/pulsepass/tests/e2e/checklist-e2e-manual.md) para llevar el seguimiento de cobertura.
- Si una prueba no puede ejecutarse por falta de una funcionalidad previa (ej. ausencia de AuthController en Fase 3), márquela obligatoriamente como **`BLOCKED`**.
