package com.juan.reservas.reserva.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Reserva hecha por un cliente desde la página pública.
 *
 * @param recursoId el recurso del hueco elegido (con "sin preferencia", el que asignó la página)
 * @param inicio    instante UTC de uno de los huecos ofrecidos por la disponibilidad
 * @param telefono  opcional
 */
public record ReservaPublicaRequest(
        @NotNull UUID servicioId,
        @NotNull UUID recursoId,
        @NotNull Instant inicio,
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Email @Size(max = 160) String email,
        @Pattern(regexp = "^([0-9+()\s-]{6,30})?$", message = "solo números, espacios y + ( ) -")
        String telefono,
        @Size(max = 500) String notas) {
}
