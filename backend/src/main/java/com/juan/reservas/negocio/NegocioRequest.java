package com.juan.reservas.negocio;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos y reglas que el propietario puede cambiar desde Ajustes.
 * <p>
 * El slug y la zona horaria no se editan aquí: el slug es la URL que ya tienen sus clientes,
 * y cambiar la zona movería todas las horas de los horarios y las excepciones.
 *
 * @param antelacionMinimaMin    minutos mínimos entre ahora y el inicio de una reserva (hasta 7 días)
 * @param antelacionMaximaDias   días como mucho entre hoy y la reserva
 * @param plazoCancelacionHoras  horas antes de la cita hasta las que el cliente puede cancelar online
 * @param confirmacionAutomatica false: las reservas de la web llegan PENDIENTES y el negocio las confirma
 */
public record NegocioRequest(
        @NotBlank @Size(max = 120) String nombre,
        @Email @Size(max = 160) String email,
        @Size(max = 30) String telefono,
        @Size(max = 255) String direccion,
        @NotBlank @Size(max = 40) String etiquetaRecurso,
        @NotNull @Min(0) @Max(10080) Integer antelacionMinimaMin,
        @NotNull @Min(1) @Max(365) Integer antelacionMaximaDias,
        @NotNull @Min(0) @Max(720) Integer plazoCancelacionHoras,
        @NotNull Boolean confirmacionAutomatica) {
}
