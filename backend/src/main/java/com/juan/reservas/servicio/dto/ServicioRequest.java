package com.juan.reservas.servicio.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta o edición de un servicio.
 *
 * @param activo opcional: al crear, el servicio nace activo; al editar, {@code null} lo deja
 *               como está y {@code true} lo reactiva
 */
public record ServicioRequest(
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 1000) String descripcion,
        @NotNull @Min(1) @Max(480) Integer duracionMin,
        @NotNull @Min(0) @Max(240) Integer margenMin,
        @NotNull @Min(0) @Max(10_000_000) Integer precioCentimos,
        Boolean activo) {
}
