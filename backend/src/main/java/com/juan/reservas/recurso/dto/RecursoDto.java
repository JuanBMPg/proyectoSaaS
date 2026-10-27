package com.juan.reservas.recurso.dto;

import java.util.Set;
import java.util.UUID;

import com.juan.reservas.recurso.TipoRecurso;

/** Incluye los servicios que presta para que el panel no necesite otra llamada. */
public record RecursoDto(
        UUID id,
        String nombre,
        TipoRecurso tipo,
        boolean activo,
        Set<UUID> servicioIds) {
}
