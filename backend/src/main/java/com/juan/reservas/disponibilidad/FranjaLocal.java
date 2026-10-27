package com.juan.reservas.disponibilidad;

import java.time.LocalTime;
import java.util.Objects;

/** Franja del horario en hora local del negocio, p. ej. 09:00-14:00. No cruza la medianoche. */
public record FranjaLocal(LocalTime inicio, LocalTime fin) {

    public FranjaLocal {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fin, "fin");
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException("El inicio debe ser anterior al fin: " + inicio + " - " + fin);
        }
    }
}
