package com.juan.reservas.disponibilidad;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * Todo lo que necesita la calculadora, ya cargado: no consulta la base de datos ni el reloj.
 *
 * @param fecha                día pedido, en la zona horaria del negocio
 * @param zona                 zona horaria del negocio (p. ej. Europe/Madrid)
 * @param duracion             duración del servicio
 * @param margen               tiempo que el recurso queda bloqueado después del servicio
 * @param ahora                instante actual (en producción viene del Clock inyectado)
 * @param antelacionMinima     no se puede reservar con menos antelación que esta
 * @param antelacionMaximaDias no se puede reservar a más de estos días (de calendario)
 * @param paso                 separación entre los inicios posibles (15 min por defecto)
 * @param recursos             recursos que pueden prestar el servicio, con su agenda del día
 */
public record SolicitudDisponibilidad(
        LocalDate fecha,
        ZoneId zona,
        Duration duracion,
        Duration margen,
        Instant ahora,
        Duration antelacionMinima,
        int antelacionMaximaDias,
        Duration paso,
        List<AgendaRecurso> recursos) {

    public static final Duration PASO_POR_DEFECTO = Duration.ofMinutes(15);

    public SolicitudDisponibilidad {
        Objects.requireNonNull(fecha, "fecha");
        Objects.requireNonNull(zona, "zona");
        Objects.requireNonNull(ahora, "ahora");
        exigirPositiva(duracion, "duracion");
        exigirPositiva(paso, "paso");
        exigirNoNegativa(margen, "margen");
        exigirNoNegativa(antelacionMinima, "antelacionMinima");
        if (antelacionMaximaDias < 0) {
            throw new IllegalArgumentException("antelacionMaximaDias no puede ser negativa");
        }
        recursos = List.copyOf(recursos);
    }

    /** Tiempo que ocupa cada reserva en la agenda del recurso: el servicio más su margen. */
    public Duration bloque() {
        return duracion.plus(margen);
    }

    private static void exigirPositiva(Duration duracion, String nombre) {
        Objects.requireNonNull(duracion, nombre);
        if (duracion.isNegative() || duracion.isZero()) {
            throw new IllegalArgumentException(nombre + " debe ser positiva");
        }
    }

    private static void exigirNoNegativa(Duration duracion, String nombre) {
        Objects.requireNonNull(duracion, nombre);
        if (duracion.isNegative()) {
            throw new IllegalArgumentException(nombre + " no puede ser negativa");
        }
    }
}
