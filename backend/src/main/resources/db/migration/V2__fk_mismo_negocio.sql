-- Garantiza en la base de datos que el servicio, el recurso y el cliente de una reserva
-- pertenecen al mismo negocio que la propia reserva (aislamiento multi-tenant).
--
-- Con las FKs simples de V1 se podía guardar una reserva del negocio A con un recurso
-- del negocio B. Con FKs compuestas (x_id, negocio_id) eso es imposible.

-- 1. Una FK compuesta necesita una restricción UNIQUE sobre esas columnas en la tabla
--    referenciada. (id ya es único por ser PK, así que no cambia qué datos son válidos.)
ALTER TABLE servicio ADD CONSTRAINT servicio_id_negocio_uk UNIQUE (id, negocio_id);
ALTER TABLE recurso  ADD CONSTRAINT recurso_id_negocio_uk  UNIQUE (id, negocio_id);
ALTER TABLE cliente  ADD CONSTRAINT cliente_id_negocio_uk  UNIQUE (id, negocio_id);

-- 2. Sustituir las FKs simples de reserva (nombres generados por PostgreSQL en V1)
--    por FKs compuestas que incluyen negocio_id.
ALTER TABLE reserva
  DROP CONSTRAINT reserva_servicio_id_fkey,
  DROP CONSTRAINT reserva_recurso_id_fkey,
  DROP CONSTRAINT reserva_cliente_id_fkey,
  ADD CONSTRAINT reserva_servicio_mismo_negocio_fk
    FOREIGN KEY (servicio_id, negocio_id) REFERENCES servicio (id, negocio_id),
  ADD CONSTRAINT reserva_recurso_mismo_negocio_fk
    FOREIGN KEY (recurso_id, negocio_id) REFERENCES recurso (id, negocio_id),
  ADD CONSTRAINT reserva_cliente_mismo_negocio_fk
    FOREIGN KEY (cliente_id, negocio_id) REFERENCES cliente (id, negocio_id);
