package com.juan.reservas.servicio.dto;

import java.util.UUID;

public record ServicioDto(
        UUID id,
        String nombre,
        String descripcion,
        int duracionMin,
        int margenMin,
        int precioCentimos,
        boolean activo) {
}
