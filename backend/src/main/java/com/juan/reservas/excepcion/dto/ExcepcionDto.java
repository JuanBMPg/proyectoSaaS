package com.juan.reservas.excepcion.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Excepción del horario tal como la ve el propietario.
 *
 * @param recurso   null si afecta a todo el negocio
 * @param horaInicio null (con horaFin null) si es un cierre; horas como "10:00"
 */
public record ExcepcionDto(
        UUID id,
        RecursoAfectado recurso,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
        @JsonFormat(pattern = "HH:mm") LocalTime horaFin,
        String motivo) {

    public record RecursoAfectado(UUID id, String nombre) {
    }
}
