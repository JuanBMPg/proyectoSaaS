-- Excepciones al horario semanal (fase 2): vacaciones, festivos y horarios especiales.
--
-- Cada fila cubre un rango de días [fecha_inicio, fecha_fin], ambos incluidos, en la zona
-- horaria del negocio (DATE y TIME locales, igual que el horario semanal).
--   * Sin horas (hora_inicio y hora_fin NULL): cerrado esos días.
--   * Con horas: esos días se atiende SOLO en esa franja, en lugar del horario semanal.
--   * recurso_id NULL: afecta a todo el negocio (un festivo); con valor, a un recurso (vacaciones).

CREATE TABLE excepcion (
  id            UUID PRIMARY KEY,
  negocio_id    UUID NOT NULL REFERENCES negocio(id) ON DELETE CASCADE,
  recurso_id    UUID,
  fecha_inicio  DATE NOT NULL,
  fecha_fin     DATE NOT NULL,
  hora_inicio   TIME,
  hora_fin      TIME,
  motivo        VARCHAR(120) NOT NULL,
  creado_en     TIMESTAMPTZ NOT NULL,
  CONSTRAINT excepcion_fechas_ck CHECK (fecha_inicio <= fecha_fin),
  -- Las dos horas a la vez (y en orden) o ninguna
  CONSTRAINT excepcion_horas_ck CHECK (
    (hora_inicio IS NULL AND hora_fin IS NULL)
    OR (hora_inicio IS NOT NULL AND hora_fin IS NOT NULL AND hora_inicio < hora_fin)
  ),
  -- Mismo patrón que V2: el recurso tiene que ser del mismo negocio. Con recurso_id NULL
  -- la FK no se comprueba (MATCH SIMPLE), que es justo el caso "todo el negocio".
  CONSTRAINT excepcion_recurso_mismo_negocio_fk
    FOREIGN KEY (recurso_id, negocio_id) REFERENCES recurso (id, negocio_id) ON DELETE CASCADE
);

-- La disponibilidad busca las excepciones de un negocio que cubren un día concreto
CREATE INDEX idx_excepcion_negocio_fechas ON excepcion (negocio_id, fecha_inicio, fecha_fin);
