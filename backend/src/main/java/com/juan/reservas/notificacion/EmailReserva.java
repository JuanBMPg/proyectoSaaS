package com.juan.reservas.notificacion;

/** Email ya redactado: asunto y las dos versiones del cuerpo. */
public record EmailReserva(String para, String asunto, String texto, String html) {
}
