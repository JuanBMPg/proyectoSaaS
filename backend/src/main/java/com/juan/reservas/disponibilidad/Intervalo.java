package com.juan.reservas.disponibilidad;

import java.time.Instant;
import java.util.Objects;

/**
 * Intervalo de tiempo semiabierto [inicio, fin), igual que los rangos de las restricciones
 * de PostgreSQL: un intervalo que acaba a las 10:00 y otro que empieza a las 10:00 no se solapan.
 */
public record Intervalo(Instant inicio, Instant fin) {

    public Intervalo {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fin, "fin");
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException("El inicio debe ser anterior al fin: " + inicio + " - " + fin);
        }
    }

    public boolean seSolapaCon(Intervalo otro) {
        return inicio.isBefore(otro.fin) && otro.inicio.isBefore(fin);
    }
}
