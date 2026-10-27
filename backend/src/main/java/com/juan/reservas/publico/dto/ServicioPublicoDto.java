package com.juan.reservas.publico.dto;

import java.util.List;
import java.util.UUID;

/** Servicio reservable. Sin margen: es un detalle interno del negocio. */
public record ServicioPublicoDto(
        UUID id,
        String nombre,
        String descripcion,
        int duracionMin,
        int precioCentimos,
        List<UUID> recursoIds) {
}
