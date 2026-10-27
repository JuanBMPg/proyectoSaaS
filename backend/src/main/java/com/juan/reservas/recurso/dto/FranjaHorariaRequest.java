package com.juan.reservas.recurso.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Una franja del horario semanal.
 *
 * @param diaSemana  1 = lunes ... 7 = domingo (ISO 8601)
 * @param horaInicio hora local del negocio, p. ej. "09:00"
 * @param horaFin    hora local del negocio, posterior a horaInicio (no cruza la medianoche)
 */
public record FranjaHorariaRequest(
        @NotNull @Min(1) @Max(7) Integer diaSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin) {

    /** Bean Validation trata este método como una propiedad booleana más ("inicioAntesDeFin"). */
    @AssertTrue(message = "la hora de inicio debe ser anterior a la de fin")
    public boolean isInicioAntesDeFin() {
        // Si falta alguna hora ya lo señala @NotNull; aquí no se duplica el error
        return horaInicio == null || horaFin == null || horaInicio.isBefore(horaFin);
    }

    /** Rango [inicio, fin): dos franjas contiguas (09-14 y 14-18) no se solapan. */
    public boolean seSolapaCon(FranjaHorariaRequest otra) {
        return diaSemana.equals(otra.diaSemana)
                && horaInicio.isBefore(otra.horaFin)
                && otra.horaInicio.isBefore(horaFin);
    }
}
