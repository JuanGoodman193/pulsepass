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

## Ejecutar

Requisitos: JDK 21, Maven 3.9+ y PostgreSQL para ejecutar la aplicación. Las pruebas de integración de persistencia requieren Docker porque Testcontainers inicia PostgreSQL automáticamente; las pruebas unitarias de servicios no requieren base de datos.

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

```bash
mvn clean test
```

Las pruebas no usan H2. `PersistenceIntegrationTest` verifica las migraciones Flyway, asociaciones JPA, Query Methods, JPQL, conteo de tickets pagados y una restricción `UNIQUE` real de PostgreSQL. Los tests de servicio usan JUnit 5, Mockito y AssertJ para probar las reglas sin Spring ApplicationContext ni PostgreSQL.
