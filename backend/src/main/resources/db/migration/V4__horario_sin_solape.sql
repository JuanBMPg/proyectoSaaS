-- Garantiza en la base de datos que las franjas horarias de un recurso no se solapan
-- dentro del mismo día, igual que reserva_sin_solape hace con las reservas.

-- PostgreSQL trae tipos rango para fechas e instantes (tsrange, tstzrange), pero no para
-- TIME, así que se crea uno. La función de diferencia permite al índice GiST organizar los
-- rangos de forma eficiente (es el ejemplo de la documentación oficial de PostgreSQL).
CREATE FUNCTION time_subtype_diff(x time, y time) RETURNS float8
  AS 'SELECT EXTRACT(EPOCH FROM (x - y))' LANGUAGE sql STRICT IMMUTABLE;

CREATE TYPE timerange AS RANGE (subtype = time, subtype_diff = time_subtype_diff);

-- Rango [inicio, fin): 09:00-14:00 y 14:00-18:00 son contiguas y NO se solapan.
-- btree_gist (creada en V1) permite combinar "=" sobre uuid y smallint con "&&" sobre rangos.
ALTER TABLE horario ADD CONSTRAINT horario_sin_solape EXCLUDE USING gist (
  recurso_id WITH =,
  dia_semana WITH =,
  timerange(hora_inicio, hora_fin, '[)') WITH &&
);
