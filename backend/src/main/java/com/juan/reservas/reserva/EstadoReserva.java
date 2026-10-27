package com.juan.reservas.reserva;

import java.util.Set;

/** Ciclo de vida de una reserva (CHECK en la tabla reserva). */
public enum EstadoReserva {
    /** Espera a que el negocio la confirme (si la confirmación es manual). */
    PENDIENTE,
    /** Cita en firme. */
    CONFIRMADA,
    /** Anulada por el cliente o el negocio; libera el hueco. */
    CANCELADA,
    /** El cliente vino. */
    COMPLETADA,
    /** El cliente no vino. */
    NO_PRESENTADO;

    /**
     * Estados que ocupan el hueco. Son los mismos que el WHERE de la restricción
     * reserva_sin_solape: lo que la calculadora considera ocupado es lo que la BD protege.
     */
    public static final Set<EstadoReserva> ACTIVOS = Set.of(PENDIENTE, CONFIRMADA);
}
