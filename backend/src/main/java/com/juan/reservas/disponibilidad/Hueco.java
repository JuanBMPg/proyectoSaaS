package com.juan.reservas.disponibilidad;

import java.time.Instant;
import java.util.UUID;

/**
 * Hora a la que se puede empezar el servicio con un recurso.
 * {@code fin} es el final del servicio que verá el cliente, sin el margen posterior.
 */
public record Hueco(UUID recursoId, Instant inicio, Instant fin) {
}
