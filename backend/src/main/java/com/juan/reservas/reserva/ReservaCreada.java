package com.juan.reservas.reserva;

import java.util.UUID;

/**
 * Evento de dominio: se ha creado una reserva desde la web pública.
 *
 * @param token token en claro del enlace de la reserva. En la base de datos solo está su hash,
 *              así que es la única forma de incluirlo en el email; viaja solo en memoria
 */
public record ReservaCreada(UUID reservaId, String token) {

    /** Sin el token: un evento registrado en los logs no debe filtrar el enlace secreto. */
    @Override
    public String toString() {
        return "ReservaCreada[reservaId=" + reservaId + "]";
    }
}
