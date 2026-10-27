package com.juan.reservas.recurso.dto;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** Conjunto COMPLETO de servicios que presta el recurso; sustituye al anterior. */
public record AsignacionServiciosRequest(@NotNull Set<@NotNull UUID> servicioIds) {
}
