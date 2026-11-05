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

    /**
     * Cambios que puede hacer el negocio desde su panel. Una reserva cancelada no se reactiva
     * (se crea otra: el hueco puede haberse ocupado); completada y no presentado se pueden
     * corregir entre sí por si se marcó la equivocada.
     */
    public boolean puedePasarA(EstadoReserva nuevo) {
        return switch (this) {
            case PENDIENTE -> nuevo == CONFIRMADA || nuevo == CANCELADA;
            case CONFIRMADA -> nuevo == CANCELADA || nuevo == COMPLETADA || nuevo == NO_PRESENTADO;
            case COMPLETADA -> nuevo == NO_PRESENTADO;
            case NO_PRESENTADO -> nuevo == COMPLETADA;
            case CANCELADA -> false;
        };
    }
}
