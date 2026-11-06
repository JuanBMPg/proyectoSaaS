package com.juan.reservas.reserva;

import java.util.UUID;

/**
 * Evento de dominio: el negocio ha confirmado una reserva que estaba pendiente (confirmación
 * manual). Se publica dentro de la transacción del cambio, como los demás.
 */
public record ReservaConfirmada(UUID reservaId) {
}
