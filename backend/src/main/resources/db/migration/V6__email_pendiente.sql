-- Patrón outbox para los emails (fase 2).
--
-- Antes, el email de confirmación se enviaba en otro hilo después del commit: si la aplicación
-- se caía entre el commit y el envío, ese email se perdía. Ahora el email ya redactado se
-- guarda en esta tabla DENTRO de la misma transacción que la reserva (o se guardan los dos o
-- ninguno) y una tarea programada lo envía, reintentando si el servidor de correo falla.

CREATE TABLE email_pendiente (
  id                UUID PRIMARY KEY,
  negocio_id        UUID NOT NULL REFERENCES negocio(id) ON DELETE CASCADE,
  reserva_id        UUID REFERENCES reserva(id) ON DELETE CASCADE,
  tipo              VARCHAR(30)  NOT NULL CHECK (tipo IN
                      ('RESERVA_CREADA','RECORDATORIO','RESERVA_CONFIRMADA','RESERVA_CANCELADA')),
  para              VARCHAR(160) NOT NULL,
  nombre_remitente  VARCHAR(120) NOT NULL,
  responder_a       VARCHAR(160),
  asunto            VARCHAR(255) NOT NULL,
  -- Cuerpos del email. Pueden llevar el enlace secreto de la reserva, así que se vacían al
  -- enviarse: el token en claro solo vive en la base de datos hasta que sale el correo.
  texto             TEXT,
  html              TEXT,
  estado            VARCHAR(20)  NOT NULL CHECK (estado IN ('PENDIENTE','ENVIADO','FALLIDO')),
  intentos          INT          NOT NULL DEFAULT 0,
  proximo_intento   TIMESTAMPTZ  NOT NULL,
  ultimo_error      VARCHAR(500),
  creado_en         TIMESTAMPTZ  NOT NULL,
  enviado_en        TIMESTAMPTZ
);

-- La cola: solo los pendientes, por orden de envío. Índice parcial, pequeño aunque la tabla crezca.
CREATE INDEX idx_email_pendiente_cola ON email_pendiente (proximo_intento) WHERE estado = 'PENDIENTE';

-- Como mucho un recordatorio por reserva, aunque la tarea se ejecute en dos instancias a la vez
CREATE UNIQUE INDEX email_pendiente_un_recordatorio_uk ON email_pendiente (reserva_id) WHERE tipo = 'RECORDATORIO';
