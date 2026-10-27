-- Garantiza en la base de datos que un recurso solo puede prestar servicios de su propio negocio.
--
-- Con V1, servicio_recurso podía enlazar un servicio del negocio A con un recurso del negocio B.
-- Se añade negocio_id y se sustituyen las FKs simples por FKs compuestas (x_id, negocio_id),
-- igual que hizo V2 con reserva. Las restricciones UNIQUE (id, negocio_id) de servicio y
-- recurso que necesitan estas FKs ya las creó V2.

-- 1. Nueva columna, rellenada a partir del servicio para que la migración sea válida
--    aunque la tabla ya tuviera filas.
ALTER TABLE servicio_recurso ADD COLUMN negocio_id UUID;

UPDATE servicio_recurso sr
   SET negocio_id = s.negocio_id
  FROM servicio s
 WHERE s.id = sr.servicio_id;

ALTER TABLE servicio_recurso ALTER COLUMN negocio_id SET NOT NULL;

-- 2. FKs compuestas. Se mantiene ON DELETE CASCADE: si se borra el servicio o el recurso,
--    desaparece la asignación.
ALTER TABLE servicio_recurso
  DROP CONSTRAINT servicio_recurso_servicio_id_fkey,
  DROP CONSTRAINT servicio_recurso_recurso_id_fkey,
  ADD CONSTRAINT servicio_recurso_servicio_mismo_negocio_fk
    FOREIGN KEY (servicio_id, negocio_id) REFERENCES servicio (id, negocio_id) ON DELETE CASCADE,
  ADD CONSTRAINT servicio_recurso_recurso_mismo_negocio_fk
    FOREIGN KEY (recurso_id, negocio_id) REFERENCES recurso (id, negocio_id) ON DELETE CASCADE;
