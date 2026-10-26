-- Esquema inicial (fase 1). Fuente: "Reservas para pequeños negocios — Documento de proyecto".
-- Instantes en UTC (timestamptz); horarios semanales en hora local del negocio (time).

-- Necesaria para combinar "=" sobre UUID y "&&" sobre rangos en un índice GiST
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE negocio (
  id                       UUID PRIMARY KEY,
  nombre                   VARCHAR(120) NOT NULL,
  slug                     VARCHAR(80)  NOT NULL UNIQUE,
  zona_horaria             VARCHAR(50)  NOT NULL DEFAULT 'Europe/Madrid',
  email                    VARCHAR(160),
  telefono                 VARCHAR(30),
  direccion                VARCHAR(255),
  etiqueta_recurso         VARCHAR(40)  NOT NULL DEFAULT 'Profesional',
  antelacion_minima_min    INT          NOT NULL DEFAULT 120,
  antelacion_maxima_dias   INT          NOT NULL DEFAULT 30,
  plazo_cancelacion_horas  INT          NOT NULL DEFAULT 24,
  confirmacion_automatica  BOOLEAN      NOT NULL DEFAULT TRUE,
  creado_en                TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE recurso (
  id          UUID PRIMARY KEY,
  negocio_id  UUID NOT NULL REFERENCES negocio(id) ON DELETE CASCADE,
  nombre      VARCHAR(120) NOT NULL,
  tipo        VARCHAR(20)  NOT NULL CHECK (tipo IN ('PERSONA','COSA')),
  activo      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE usuario (
  id             UUID PRIMARY KEY,
  negocio_id     UUID NOT NULL REFERENCES negocio(id) ON DELETE CASCADE,
  recurso_id     UUID REFERENCES recurso(id) ON DELETE SET NULL,
  email          VARCHAR(160) NOT NULL UNIQUE,
  password_hash  VARCHAR(100) NOT NULL,
  nombre         VARCHAR(120) NOT NULL,
  rol            VARCHAR(20)  NOT NULL CHECK (rol IN ('PROPIETARIO','EMPLEADO')),
  activo         BOOLEAN      NOT NULL DEFAULT TRUE,
  creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE servicio (
  id               UUID PRIMARY KEY,
  negocio_id       UUID NOT NULL REFERENCES negocio(id) ON DELETE CASCADE,
  nombre           VARCHAR(120) NOT NULL,
  descripcion      TEXT,
  duracion_min     INT NOT NULL CHECK (duracion_min > 0),
  margen_min       INT NOT NULL DEFAULT 0 CHECK (margen_min >= 0),
  precio_centimos  INT NOT NULL DEFAULT 0 CHECK (precio_centimos >= 0),
  activo           BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE servicio_recurso (
  servicio_id  UUID NOT NULL REFERENCES servicio(id) ON DELETE CASCADE,
  recurso_id   UUID NOT NULL REFERENCES recurso(id) ON DELETE CASCADE,
  PRIMARY KEY (servicio_id, recurso_id)
);

CREATE TABLE horario (
  id           UUID PRIMARY KEY,
  recurso_id   UUID NOT NULL REFERENCES recurso(id) ON DELETE CASCADE,
  dia_semana   SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 7), -- 1 = lunes
  hora_inicio  TIME NOT NULL,
  hora_fin     TIME NOT NULL,
  CHECK (hora_inicio < hora_fin)
);

CREATE TABLE cliente (
  id          UUID PRIMARY KEY,
  negocio_id  UUID NOT NULL REFERENCES negocio(id) ON DELETE CASCADE,
  nombre      VARCHAR(120) NOT NULL,
  email       VARCHAR(160) NOT NULL,
  telefono    VARCHAR(30),
  creado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (negocio_id, email)
);

CREATE TABLE reserva (
  id                  UUID PRIMARY KEY,
  negocio_id          UUID NOT NULL REFERENCES negocio(id) ON DELETE CASCADE,
  servicio_id         UUID NOT NULL REFERENCES servicio(id),
  recurso_id          UUID NOT NULL REFERENCES recurso(id),
  cliente_id          UUID NOT NULL REFERENCES cliente(id),
  inicio              TIMESTAMPTZ NOT NULL,
  fin                 TIMESTAMPTZ NOT NULL,
  fin_bloqueo         TIMESTAMPTZ NOT NULL, -- fin + margen del servicio
  estado              VARCHAR(20) NOT NULL CHECK (estado IN
                        ('PENDIENTE','CONFIRMADA','CANCELADA','COMPLETADA','NO_PRESENTADO')),
  notas               TEXT,
  token_cancelacion   VARCHAR(64) NOT NULL UNIQUE,
  creado_en           TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (inicio < fin AND fin <= fin_bloqueo),
  -- Ningún recurso puede tener dos reservas activas que se solapen
  CONSTRAINT reserva_sin_solape EXCLUDE USING gist (
    recurso_id WITH =,
    tstzrange(inicio, fin_bloqueo, '[)') WITH &&
  ) WHERE (estado IN ('PENDIENTE','CONFIRMADA'))
);

CREATE TABLE refresh_token (
  id           UUID PRIMARY KEY,
  usuario_id   UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
  token_hash   VARCHAR(100) NOT NULL UNIQUE,
  expira_en    TIMESTAMPTZ NOT NULL,
  revocado     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_reserva_recurso_inicio ON reserva (recurso_id, inicio);
CREATE INDEX idx_reserva_negocio_inicio ON reserva (negocio_id, inicio);
CREATE INDEX idx_horario_recurso_dia    ON horario (recurso_id, dia_semana);
