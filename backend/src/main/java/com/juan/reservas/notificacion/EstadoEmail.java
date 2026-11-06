package com.juan.reservas.notificacion;

/** Situación de un email en la cola (CHECK en la tabla email_pendiente). */
public enum EstadoEmail {
    /** Por enviar, o esperando a reintentar. */
    PENDIENTE,
    ENVIADO,
    /** Agotó los reintentos: queda registrado para revisarlo. */
    FALLIDO
}
