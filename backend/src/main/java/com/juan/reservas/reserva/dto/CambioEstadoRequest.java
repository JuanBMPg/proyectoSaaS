package com.juan.reservas.reserva.dto;

import com.juan.reservas.reserva.EstadoReserva;

import jakarta.validation.constraints.NotNull;

public record CambioEstadoRequest(@NotNull EstadoReserva estado) {
}
