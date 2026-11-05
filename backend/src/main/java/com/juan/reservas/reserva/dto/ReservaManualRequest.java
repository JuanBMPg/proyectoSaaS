package com.juan.reservas.reserva.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Reserva creada por el personal desde el panel (p. ej. por teléfono). A diferencia de la web
 * pública, no se aplican la antelación ni la rejilla del horario: el negocio decide. Lo único
 * que no se puede saltar es el solape con otra reserva activa.
 */
public record ReservaManualRequest(
        @NotNull UUID servicioId,
        @NotNull UUID recursoId,
        @NotNull Instant inicio,
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Email @Size(max = 160) String email,
        @Pattern(regexp = "^([0-9+()\\s-]{6,30})?$", message = "solo números, espacios y + ( ) -")
        String telefono,
        @Size(max = 500) String notas) {
}
