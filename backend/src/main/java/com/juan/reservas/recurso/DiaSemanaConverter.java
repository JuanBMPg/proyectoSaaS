package com.juan.reservas.recurso;

import java.time.DayOfWeek;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Guarda {@link DayOfWeek} como número ISO 8601 (1 = lunes ... 7 = domingo) en la columna
 * smallint dia_semana. Sin el conversor, JPA lo guardaría por su posición (0 = lunes) o por
 * su nombre, y no coincidiría con el CHECK (dia_semana BETWEEN 1 AND 7) de la tabla.
 */
@Converter
public class DiaSemanaConverter implements AttributeConverter<DayOfWeek, Short> {

    @Override
    public Short convertToDatabaseColumn(DayOfWeek dia) {
        return dia == null ? null : (short) dia.getValue();
    }

    @Override
    public DayOfWeek convertToEntityAttribute(Short valor) {
        return valor == null ? null : DayOfWeek.of(valor);
    }
}
