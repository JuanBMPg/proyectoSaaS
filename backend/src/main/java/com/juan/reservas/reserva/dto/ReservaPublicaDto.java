package com.juan.reservas.reserva.dto;

import java.time.Instant;

import com.juan.reservas.reserva.EstadoReserva;

/**
 * Una reserva vista desde su enlace secreto. Sin datos de contacto del cliente: quien tiene
 * el enlace ya los conoce, y así un enlace reenviado no expone su email ni su teléfono.
 */
public record ReservaPublicaDto(
        EstadoReserva estado,
        Instant inicio,
        Instant fin,
        String clienteNombre,
        Servicio servicio,
        Recurso recurso,
        Negocio negocio) {

    public record Servicio(String nombre, int duracionMin, int precioCentimos) {
    }

    /** @param etiqueta cómo llama el negocio a sus recursos ("Profesional", "Pista"...) */
    public record Recurso(String nombre, String etiqueta) {
    }

    public record Negocio(String nombre, String slug, String zonaHoraria, String direccion, String telefono) {
    }
}
