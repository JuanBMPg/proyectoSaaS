package com.juan.reservas.notificacion;

/** Qué email es (CHECK en la tabla email_pendiente). */
public enum TipoEmail {
    /** Al reservar: confirmada o, si el negocio confirma a mano, solicitud recibida. */
    RESERVA_CREADA,
    /** Unas horas antes de la cita. */
    RECORDATORIO,
    /** El negocio ha confirmado una reserva pendiente. */
    RESERVA_CONFIRMADA,
    /** La reserva se ha cancelado (por el cliente o por el negocio). */
    RESERVA_CANCELADA
}
