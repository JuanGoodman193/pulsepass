-- ==============================================================================
-- PulsePass — Script de Semilla para Pruebas E2E Manuales (seed-e2e.sql)
-- Entorno: Exclusivamente base de datos de pruebas 'pulsepass_e2e'
-- ==============================================================================
-- IMPORTANTE:
-- 1. Este script DEBE ejecutarse únicamente sobre la base de datos de pruebas 'pulsepass_e2e'.
-- 2. No ejecutar sobre bases de datos de desarrollo compartido, QA ni producción.
-- 3. Requiere que las migraciones Flyway V1, V2 y V3 se hayan aplicado previamente.
-- 4. Es idempotente: puede ejecutarse repetidamente sin generar errores de clave primaria.
-- ==============================================================================

-- 1. VENUES DE PRUEBA
-- Venues activos e inactivos para verificar catálogos y reglas de negocio
INSERT INTO venues (code, name, city, address, capacity, active)
VALUES 
    ('VEN-SMR-01', 'Marina Center Santa Marta', 'Santa Marta', 'Calle 10 # 5-20, El Rodadero', 500, TRUE),
    ('VEN-BOG-01', 'Movistar Arena Bogota', 'Bogota', 'Diagonal 61C # 26-36', 14000, TRUE),
    ('VEN-INA-99', 'Teatro Central Inactivo', 'Medellin', 'Carrera 45 # 50-10', 300, FALSE)
ON CONFLICT (code) DO UPDATE 
SET name = EXCLUDED.name,
    city = EXCLUDED.city,
    address = EXCLUDED.address,
    capacity = EXCLUDED.capacity,
    active = EXCLUDED.active;

-- 2. ARTISTAS ADICIONALES DE PRUEBA
-- Nota: La migración Flyway V2 ya insertó los artistas 1 al 5 ('Solar Beat', 'Neon Waves', etc.)
-- Insertamos un artista inactivo para pruebas negativas de reglas de negocio
INSERT INTO artists (stage_name, country, genre, active)
VALUES 
    ('Silencioso Inactivo', 'Colombia', 'Instrumental', FALSE)
ON CONFLICT (stage_name) DO UPDATE 
SET country = EXCLUDED.country,
    genre = EXCLUDED.genre,
    active = EXCLUDED.active;

-- 3. USUARIOS Y PERFILES DE PRUEBA (FIXTURES)
-- Proveemos identidades base para pruebas de consulta, autenticación y propiedad.
-- En la Fase 4, si se incorporan las columnas 'password' y 'roles', este bloque
-- se complementará con los hashes BCrypt correspondientes.
INSERT INTO users (username, email, active)
VALUES 
    ('andrea_e2e', 'andrea.pruebas@example.test', TRUE),
    ('bernardo_e2e', 'bernardo.pruebas@example.test', TRUE),
    ('inactivo_e2e', 'inactivo.pruebas@example.test', FALSE)
ON CONFLICT (username) DO NOTHING;

-- Perfiles de usuario vinculados
INSERT INTO user_profiles (first_name, last_name, phone, city, birth_date, user_id)
SELECT 'Andrea', 'Pruebas', '+573001234567', 'Santa Marta', '1998-05-15', u.id
FROM users u WHERE u.username = 'andrea_e2e'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO user_profiles (first_name, last_name, phone, city, birth_date, user_id)
SELECT 'Bernardo', 'Pruebas', '+573007654321', 'Bogota', '1995-08-20', u.id
FROM users u WHERE u.username = 'bernardo_e2e'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO user_profiles (first_name, last_name, phone, city, birth_date, user_id)
SELECT 'Inactivo', 'Pruebas', '+573000000000', 'Cali', '1990-01-01', u.id
FROM users u WHERE u.username = 'inactivo_e2e'
ON CONFLICT (user_id) DO NOTHING;

-- 4. EVENTOS DE PRUEBA
-- Evento publicado con fecha futura y capacidad disponible
INSERT INTO events (event_code, name, description, category, status, event_date, minimum_age, venue_id)
SELECT 
    'EVT-PUB-01',
    'Electro Fest Caribe 2027',
    'Festival anual de musica electronica frente al mar',
    'MUSIC',
    'PUBLISHED',
    '2027-12-15 20:00:00',
    18,
    v.id
FROM venues v WHERE v.code = 'VEN-SMR-01'
ON CONFLICT (event_code) DO UPDATE 
SET status = 'PUBLISHED',
    event_date = '2027-12-15 20:00:00';

-- Evento en borrador (DRAFT) para verificar que no se filtra al público
INSERT INTO events (event_code, name, description, category, status, event_date, minimum_age, venue_id)
SELECT 
    'EVT-DRAFT-01',
    'Festival Indie Oculto 2027',
    'Evento privado en fase de planificacion y borrador',
    'MUSIC',
    'DRAFT',
    '2027-10-10 19:00:00',
    18,
    v.id
FROM venues v WHERE v.code = 'VEN-SMR-01'
ON CONFLICT (event_code) DO UPDATE 
SET status = 'DRAFT',
    event_date = '2027-10-10 19:00:00';

-- Evento listo para ser publicado (DRAFT preparado para transición vía PATCH)
INSERT INTO events (event_code, name, description, category, status, event_date, minimum_age, venue_id)
SELECT 
    'EVT-TOPUB-01',
    'Conferencia Tech Summit 2027',
    'Cumbre internacional de tecnologia y desarrollo de software',
    'TECHNOLOGY',
    'DRAFT',
    '2027-11-25 09:00:00',
    0,
    v.id
FROM venues v WHERE v.code = 'VEN-BOG-01'
ON CONFLICT (event_code) DO UPDATE 
SET status = 'DRAFT',
    event_date = '2027-11-25 09:00:00';

-- 5. ASOCIACIÓN EVENTO - ARTISTA
-- Asociamos al artista 'Solar Beat' (ID 1 de V2) con el evento publicado 'EVT-PUB-01'
INSERT INTO event_artists (event_id, artist_id)
SELECT e.id, a.id
FROM events e, artists a
WHERE e.event_code = 'EVT-PUB-01' AND a.stage_name = 'Solar Beat'
ON CONFLICT (event_id, artist_id) DO NOTHING;

-- 6. TICKETS DE PRUEBA PRE-PROVISIONADOS
-- Ticket elegible para prueba manual de CANCELACIÓN (estado inicial PAID)
INSERT INTO tickets (ticket_code, type, price, status, purchase_date, user_id, event_id)
SELECT 
    'TCK-E2E-CAN01',
    'GENERAL',
    50.00,
    'PAID',
    CURRENT_TIMESTAMP,
    u.id,
    e.id
FROM users u, events e
WHERE u.username = 'andrea_e2e' AND e.event_code = 'EVT-PUB-01'
ON CONFLICT (ticket_code) DO UPDATE
SET status = 'PAID';

-- Ticket elegible para prueba manual de USO por STAFF (estado inicial PAID)
INSERT INTO tickets (ticket_code, type, price, status, purchase_date, user_id, event_id)
SELECT 
    'TCK-E2E-USE01',
    'VIP',
    100.00,
    'PAID',
    CURRENT_TIMESTAMP,
    u.id,
    e.id
FROM users u, events e
WHERE u.username = 'andrea_e2e' AND e.event_code = 'EVT-PUB-01'
ON CONFLICT (ticket_code) DO UPDATE
SET status = 'PAID';
