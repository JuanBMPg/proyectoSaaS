package com.juan.reservas.reserva.dto;

/**
 * Respuesta al crear una reserva. Es la única vez que la API devuelve el token en claro:
 * en la base de datos solo queda su hash.
 */
public record ReservaCreadaDto(String token, ReservaPublicaDto reserva) {
}
