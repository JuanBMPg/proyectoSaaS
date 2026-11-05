package com.juan.reservas.reserva.dto;

import java.time.Instant;
import java.util.UUID;

import com.juan.reservas.reserva.EstadoReserva;

/**
 * Una reserva tal como la ve el personal del negocio en su agenda: con los datos de contacto
 * del cliente (para llamarle si hace falta) y el fin del bloqueo (servicio + margen).
 */
public record ReservaPanelDto(
        UUID id,
        EstadoReserva estado,
        Instant inicio,
        Instant fin,
        Instant finBloqueo,
        String notas,
        Servicio servicio,
        Recurso recurso,
        Cliente cliente) {

    public record Servicio(UUID id, String nombre, int precioCentimos) {
    }

    public record Recurso(UUID id, String nombre) {
    }

    public record Cliente(String nombre, String email, String telefono) {
    }
}
