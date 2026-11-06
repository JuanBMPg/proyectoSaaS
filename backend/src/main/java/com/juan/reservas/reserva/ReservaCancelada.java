package com.juan.reservas.reserva;

import java.util.UUID;

/**
 * Evento de dominio: una reserva activa se ha cancelado. Se publica dentro de la transacción
 * que la cancela, así que el email de aviso se encola solo si la cancelación se guarda.
 *
 * @param porCliente true si la canceló el cliente desde su enlace; false si fue el negocio
 */
public record ReservaCancelada(UUID reservaId, boolean porCliente) {
}
