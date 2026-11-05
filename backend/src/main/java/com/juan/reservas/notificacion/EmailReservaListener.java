package com.juan.reservas.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.juan.reservas.reserva.ReservaCreada;

/**
 * Envía el email de confirmación cuando se crea una reserva.
 * <ul>
 *   <li>{@code AFTER_COMMIT}: solo se ejecuta si la transacción de la reserva se confirma. Si la
 *       base de datos la rechaza (409 por solape) o hay un rollback, no sale ningún email.</li>
 *   <li>{@code @Async}: el envío va en otro hilo; la respuesta al cliente no espera al servidor
 *       de correo.</li>
 * </ul>
 * Límite conocido: si la aplicación se cae entre el commit y el envío, ese email se pierde.
 * La solución es el patrón outbox (guardar el email pendiente en la misma transacción y
 * enviarlo con una tarea programada que reintenta), previsto para la fase 2.
 */
@Component
public class EmailReservaListener {

    private static final Logger log = LoggerFactory.getLogger(EmailReservaListener.class);

    private final NotificadorReservas notificador;

    public EmailReservaListener(NotificadorReservas notificador) {
        this.notificador = notificador;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCrearReserva(ReservaCreada evento) {
        try {
            notificador.enviarConfirmacion(evento.reservaId(), evento.token());
        } catch (MailException e) {
            // La reserva ya es válida y el cliente tiene el enlace en pantalla: se registra y sigue
            log.error("No se pudo enviar el email de confirmación de la reserva {}", evento.reservaId(), e);
        }
    }
}
