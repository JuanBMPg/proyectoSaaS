package com.juan.reservas.recurso.dto;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/** Franja del horario en las respuestas: horas como "09:00" en lugar de "09:00:00". */
public record FranjaHorariaDto(
        int diaSemana,
        @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
        @JsonFormat(pattern = "HH:mm") LocalTime horaFin) {
}
