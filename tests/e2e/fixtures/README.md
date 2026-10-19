# PulsePass — Documentación de Fixtures y Datos de Prueba E2E

Este directorio contiene los scripts y la documentación de soporte para la inicialización y restablecimiento de datos en el entorno exclusivo de pruebas de extremo a extremo (E2E) de PulsePass.

---

## 1. Alcance y Entorno de Ejecución

> [!CAUTION]
> **REGLA DE AISLAMIENTO OBLIGATORIA:**
> El script `seed-e2e.sql` y las operaciones de inicialización descritas en este documento deben ejecutarse **ÚNICAMENTE** sobre la base de datos dedicada de pruebas denominada `pulsepass_e2e`.
> **Queda terminantemente prohibido** ejecutar este script contra entornos de desarrollo compartido, ambientes de control de calidad (QA) o bases de datos de producción.

### Datos de Conexión Recomendados para E2E

| Parámetro | Valor E2E Local |
| :--- | :--- |
| **Host** | `localhost` |
| **Puerto** | `5432` |
| **Base de Datos** | `pulsepass_e2e` |
| **Usuario** | `postgres` (o usuario local configurado) |
| **Contraseña** | Según entorno local (no versionar credenciales reales) |

---

## 2. Prerrequisitos de Aplicación

Antes de aplicar el script de semilla `seed-e2e.sql`, el evaluador debe verificar:

1. **Instancia PostgreSQL activa** y accesible en el puerto configurado.
2. **Base de datos `pulsepass_e2e` creada:**
   ```sql
   CREATE DATABASE pulsepass_e2e;
   ```
3. **Historial de migraciones Flyway aplicado en su totalidad:**
   - `V1__create_schema.sql` (tablas `venues`, `events`, `artists`, `event_artists`, `users`, `user_profiles`, `tickets` e índices).
   - `V2__insert_initial_artists.sql` (inserción de 5 artistas iniciales: *Solar Beat*, *Neon Waves*, *Caribbean Sound*, *Ocean Drive*, *Digital Pulse*).
   - `V3__add_streaming_url_to_event.sql` (adición de la columna `streaming_url` en `events`).
   
   *Nota:* Al arrancar la aplicación PulsePass configurada con `spring.datasource.url=jdbc:postgresql://localhost:5432/pulsepass_e2e`, Flyway ejecutará estas migraciones de forma automática.

---

## 3. Instrucciones de Aplicación del Seed

### Opción A: Desde la línea de comandos con `psql`

```bash
psql -h localhost -p 5432 -U postgres -d pulsepass_e2e -f tests/e2e/fixtures/seed-e2e.sql
```

### Opción B: Desde clientes gráficos (DBeaver, pgAdmin, DataGrip)

1. Conectarse a la base de datos `pulsepass_e2e`.
2. Abrir un editor SQL.
3. Cargar el contenido de `tests/e2e/fixtures/seed-e2e.sql`.
4. Ejecutar el script completo.
5. Comprobar que no se hayan generado errores y que los registros existan en las tablas.

---

## 4. Inventario de Fixtures Aprovisionados

El script `seed-e2e.sql` es **idempotente** (utiliza cláusulas `ON CONFLICT DO NOTHING` / `DO UPDATE`) y garantiza la existencia de los siguientes registros deterministas:

### A. Venues (`venues`)

| Código (`code`) | Nombre | Ciudad | Capacidad | Estado | Propósito en Pruebas |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `VEN-SMR-01` | Marina Center Santa Marta | Santa Marta | 500 | `TRUE` (Activo) | Venue activo para creación de eventos y compras E2E. |
| `VEN-BOG-01` | Movistar Arena Bogota | Bogota | 14,000 | `TRUE` (Activo) | Venue activo de gran capacidad para eventos múltiples. |
| `VEN-INA-99` | Teatro Central Inactivo | Medellin | 300 | `FALSE` (Inactivo) | Verificación de rechazo de creación de eventos en venues inactivos (409 Conflict). |

### B. Artistas (`artists`)

| ID | Nombre Artístico (`stage_name`) | País | Género | Estado | Origen |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `1` | `Solar Beat` | Colombia | Electronic | `TRUE` | Flyway `V2` (asociable a eventos). |
| `2` | `Neon Waves` | Mexico | Synth Pop | `TRUE` | Flyway `V2`. |
| `3` | `Caribbean Sound` | Colombia | Caribbean Fusion | `TRUE` | Flyway `V2`. |
| `4` | `Ocean Drive` | United States | Rock | `TRUE` | Flyway `V2`. |
| `5` | `Digital Pulse` | Spain | Tech Performance | `TRUE` | Flyway `V2`. |
| — | `Silencioso Inactivo` | Colombia | Instrumental | `FALSE` | Seed E2E (pruebas negativas de negocio). |

### C. Eventos (`events`)

| Código (`event_code`) | Nombre | Categoría | Estado | Fecha del Evento | Edad Mín. | Venue | Propósito en Pruebas |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `EVT-PUB-01` | Electro Fest Caribe 2027 | `MUSIC` | `PUBLISHED` | `2027-12-15 20:00:00` | 18 | `VEN-SMR-01` | Evento publicado para consultas públicas y compra de tickets. |
| `EVT-DRAFT-01` | Festival Indie Oculto 2027 | `MUSIC` | `DRAFT` | `2027-10-10 19:00:00` | 18 | `VEN-SMR-01` | Evento borrador para verificar que no es visible al público. |
| `EVT-TOPUB-01` | Conferencia Tech Summit 2027 | `TECHNOLOGY` | `DRAFT` | `2027-11-25 09:00:00` | 0 | `VEN-BOG-01` | Evento borrador listo para probar la transición `PATCH /publish`. |

### D. Relaciones Evento - Artista (`event_artists`)

- Evento `EVT-PUB-01` vinculado con el artista `Solar Beat` (ID 1). Permite validar de inmediato la consulta `GET /api/events/by-artist?stageName=Solar Beat`.

### E. Usuarios y Perfiles Preprovisionados

| Username | Email | Perfil (Nombre, Teléfono, Ciudad, Cumpleaños) | Estado | Propósito |
| :--- | :--- | :--- | :--- | :--- |
| `andrea_e2e` | `andrea.pruebas@example.test` | Andrea Pruebas, +573001234567, Santa Marta, 1998-05-15 | `TRUE` | `USER_A` para compras, cancelaciones y consultas propias. |
| `bernardo_e2e` | `bernardo.pruebas@example.test` | Bernardo Pruebas, +573007654321, Bogota, 1995-08-20 | `TRUE` | `USER_B` para verificar escenarios de denegación cruzada (IDOR). |
| `inactivo_e2e` | `inactivo.pruebas@example.test` | Inactivo Pruebas, +573000000000, Cali, 1990-01-01 | `FALSE` | Verificación de rechazo de transacciones por cuenta inactiva. |

### F. Tickets Preprovisionados para Operaciones Irreversibles

> [!IMPORTANT]
> Las transiciones de estado de tickets (cancelación y uso) son irreversibles. Por esta razón, el seed suministra tickets independientes para cada prueba:

| Código (`ticket_code`) | Tipo | Precio | Estado Inicial | Usuario | Evento | Caso E2E Destinado |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `TCK-E2E-CAN01` | `GENERAL` | $50.00 | `PAID` | `andrea_e2e` | `EVT-PUB-01` | Destinado exclusivamente a la prueba de **cancelación** (`PATCH /cancel`). |
| `TCK-E2E-USE01` | `VIP` | $100.00 | `PAID` | `andrea_e2e` | `EVT-PUB-01` | Destinado exclusivamente a la prueba de **uso por STAFF** (`PATCH /use`). |

---

## 5. Procedimiento de Aprovisionamiento Privilegiado (STAFF y ADMIN)

Conforme a las reglas de seguridad de PulsePass:

1. **Principio de Mínimo Privilegio:** El endpoint HTTP público de registro (`POST /api/users`) asigna **únicamente** el rol `USER`. Está prohibido admitir campos de asignación de rol en el cuerpo del registro.
2. **Aprovisionamiento Fuera de Banda:** Las cuentas con roles `STAFF` y `ADMIN` deben ser provisionadas fuera de la API pública.
3. **Compatibilidad con Fases:**
   - En la **Fase 3 actual**, la tabla `users` contiene únicamente `username`, `email` y `active`. La API no cuenta con capa de seguridad Spring Security ni control de roles en base de datos.
   - En la **Fase 4 (Capa de Seguridad)**, una vez que se integre la migración correspondiente (adición de contraseñas con hash BCrypt y asignación de roles), el administrador del entorno E2E deberá ejecutar el procedimiento de inserción de credenciales seguras de prueba:
     ```sql
     -- Ejemplo ilustrativo para cuando se aplique la migración de Fase 4:
     -- INSERT INTO users (username, email, password_hash, role, active)
     -- VALUES ('admin_e2e', 'admin@example.test', '$2a$10$...BCRYPT_HASH...', 'ADMIN', TRUE);
     ```
   - **Nunca** almacenar contraseñas en texto claro ni versionar hashes productivos en el repositorio.

---

## 6. Convenciones de Datos de Prueba

- **Dominio de correo reservado:** Todo correo generado o utilizado en pruebas debe emplear el dominio reservado para pruebas según RFC 2606: `@example.test`. Nunca utilizar dominios reales (`@gmail.com`, `@hotmail.com`, etc.).
- **Identificador de ejecución (`runId`):** Para evitar colisiones en pruebas que registran usuarios o crean eventos dinámicamente, se recomienda prefijar los identificadores con el valor de la variable `{{runId}}` (ejemplo: `EVT-E2E-{{runId}}`, `user_{{runId}}@example.test`).

---

## 7. Procedimiento de Limpieza y Restablecimiento

Para repetir la sesión de pruebas manuales con un estado limpio y predecible:

1. Detener la aplicación PulsePass.
2. Reejecutar el script `seed-e2e.sql` para restaurar los estados nominales (`PAID` en los tickets preprovisionados, `DRAFT` en el evento de publicación).
3. Si se crearon eventos o tickets adicionales durante la corrida que interfieran con la capacidad o los listados:
   ```sql
   -- Limpieza controlada en pulsepass_e2e (NUNCA en desarrollo o producción):
   DELETE FROM tickets WHERE ticket_code NOT IN ('TCK-E2E-CAN01', 'TCK-E2E-USE01');
   DELETE FROM event_artists WHERE event_id IN (SELECT id FROM events WHERE event_code NOT IN ('EVT-PUB-01', 'EVT-DRAFT-01', 'EVT-TOPUB-01'));
   DELETE FROM events WHERE event_code NOT IN ('EVT-PUB-01', 'EVT-DRAFT-01', 'EVT-TOPUB-01');
   -- Re-ejecutar seed-e2e.sql
   ```
4. Iniciar la aplicación y comenzar una nueva sesión de verificación.
