-- =========================================================
--  VetSystem — Script de base de datos
--  Motor  : MySQL 8
--  Charset: utf8mb4
--  Uso    : mysql -u root -p < veterinariadb.sql
-- =========================================================

CREATE DATABASE IF NOT EXISTS veterinariadb
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE veterinariadb;

-- ---------------------------------------------------------
--  TABLA: cliente
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS cliente (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    documento   VARCHAR(20)   NOT NULL,
    nombre      VARCHAR(100)  NOT NULL,
    apellido    VARCHAR(100)  NOT NULL,
    email       VARCHAR(150)  NOT NULL,
    telefono    VARCHAR(20),
    direccion   VARCHAR(200),
    activo      TINYINT(1)    NOT NULL DEFAULT 1,
    fecha_alta  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_cliente        PRIMARY KEY (id),
    CONSTRAINT uq_cli_documento  UNIQUE (documento),
    CONSTRAINT uq_cli_email      UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------
--  TABLA: mascota
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS mascota (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(80)   NOT NULL,
    especie     VARCHAR(50)   NOT NULL,
    raza        VARCHAR(100),
    fecha_nac   DATE,
    sexo        CHAR(1),                 -- M = Macho  H = Hembra
    activo      TINYINT(1)    NOT NULL DEFAULT 1,
    cliente_id  BIGINT        NOT NULL,
    CONSTRAINT pk_mascota         PRIMARY KEY (id),
    CONSTRAINT fk_mascota_cliente FOREIGN KEY (cliente_id)
        REFERENCES cliente(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------
--  TABLA: turno
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS turno (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    fecha_hora    DATETIME      NOT NULL,
    motivo        VARCHAR(200),
    estado        VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    observaciones TEXT,
    mascota_id    BIGINT        NOT NULL,
    CONSTRAINT pk_turno         PRIMARY KEY (id),
    CONSTRAINT fk_turno_mascota FOREIGN KEY (mascota_id)
        REFERENCES mascota(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------
--  DATOS DE PRUEBA
-- ---------------------------------------------------------

-- Clientes
INSERT INTO cliente (documento, nombre, apellido, email, telefono, direccion) VALUES
  ('30111222', 'María',   'González',  'maria.gonzalez@email.com',  '11-1234-5678', 'Av. Corrientes 1234, CABA'),
  ('25333444', 'Carlos',  'Rodríguez', 'carlos.rodriguez@email.com','11-2345-6789', 'Sarmiento 567, CABA'),
  ('28555666', 'Laura',   'Martínez',  'laura.martinez@email.com',  '11-3456-7890', 'Rivadavia 890, CABA');

-- Mascotas
INSERT INTO mascota (nombre, especie, raza, fecha_nac, sexo, cliente_id) VALUES
  ('Fido',  'Perro', 'Labrador',       '2020-03-15', 'M', 1),
  ('Luna',  'Gato',  'Siamés',         '2021-07-22', 'H', 1),
  ('Rex',   'Perro', 'Pastor Alemán',  '2019-11-05', 'M', 2),
  ('Michi', 'Gato',  'Común Europeo',  '2022-01-10', 'H', 3);

-- Turnos de prueba
INSERT INTO turno (fecha_hora, motivo, estado, mascota_id) VALUES
  (DATE_ADD(NOW(), INTERVAL  1 DAY), 'Consulta general',  'PENDIENTE',  1),
  (DATE_ADD(NOW(), INTERVAL  2 DAY), 'Vacunación anual',  'CONFIRMADO', 2),
  (DATE_ADD(NOW(), INTERVAL  3 DAY), 'Desparasitación',   'PENDIENTE',  3),
  (DATE_SUB(NOW(), INTERVAL  1 DAY), 'Control de rutina', 'REALIZADO',  4);
