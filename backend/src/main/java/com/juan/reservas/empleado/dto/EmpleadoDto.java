package com.juan.reservas.empleado.dto;

import java.util.UUID;

public record EmpleadoDto(UUID id, String nombre, String email, boolean activo, Recurso recurso) {

    /** Null si el empleado no tiene recurso asignado. */
    public record Recurso(UUID id, String nombre) {
    }
}
