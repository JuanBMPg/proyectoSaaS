package com.juan.reservas.excepcion.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Nueva excepción. Sin horas = cerrado esos días; con horas = solo se atiende en esa franja.
 */
public record ExcepcionRequest(
        @Schema(description = "Recurso afectado; vacío para todo el negocio") UUID recursoId,
        @NotNull LocalDate fechaInicio,
        @Schema(description = "Último día, incluido") @NotNull LocalDate fechaFin,
        @Schema(type = "string", example = "10:00") LocalTime horaInicio,
        @Schema(type = "string", example = "14:00") LocalTime horaFin,
        @NotBlank @Size(max = 120) String motivo) {

    /** Un año como mucho: unas vacaciones o una temporada, no un cierre indefinido. */
    public static final int DIAS_MAXIMOS = 366;

    @AssertTrue(message = "la fecha de fin no puede ser anterior a la de inicio")
    public boolean isFechasEnOrden() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }

    @AssertTrue(message = "una excepción no puede durar más de " + DIAS_MAXIMOS + " días")
    public boolean isDuracionRazonable() {
        return fechaInicio == null || fechaFin == null
                || ChronoUnit.DAYS.between(fechaInicio, fechaFin) < DIAS_MAXIMOS;
    }

    @AssertTrue(message = "indica las dos horas (horario especial) o ninguna (cerrado), con el inicio antes del fin")
    public boolean isHorasCoherentes() {
        if (horaInicio == null && horaFin == null) {
            return true;
        }
        return horaInicio != null && horaFin != null && horaInicio.isBefore(horaFin);
    }
}
