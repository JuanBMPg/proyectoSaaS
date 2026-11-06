package com.juan.reservas.notificacion;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.negocio.Negocio;
import com.juan.reservas.reserva.Reserva;

/**
 * Redacta los emails de una reserva y los deja en la cola (outbox). No envía nada: eso lo hace
 * {@link EnviadorEmails}.
 * <p>
 * {@code Propagation.MANDATORY}: falla si no hay una transacción en curso. Es el contrato del
 * outbox: el email se guarda junto con el cambio que lo provoca, o no se guarda.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class NotificadorReservas {

    private final EmailPendienteRepository cola;
    private final RedactorEmailReserva redactor;
    private final Clock clock;

    public NotificadorReservas(EmailPendienteRepository cola, RedactorEmailReserva redactor, Clock clock) {
        this.cola = cola;
        this.redactor = redactor;
        this.clock = clock;
    }

    /** @param token token en claro del enlace; solo existe en memoria al crear la reserva */
    public void reservaCreada(Reserva reserva, String token) {
        encolar(reserva, TipoEmail.RESERVA_CREADA, redactor.confirmacion(reserva, token));
    }

    public void recordatorio(Reserva reserva) {
        encolar(reserva, TipoEmail.RECORDATORIO, redactor.recordatorio(reserva));
    }

    private void encolar(Reserva reserva, TipoEmail tipo, EmailReserva email) {
        Negocio negocio = reserva.getNegocio();
        cola.save(new EmailPendiente(negocio.getId(), reserva.getId(), tipo, negocio.getNombre(),
                negocio.getEmail(), email, Instant.now(clock)));
    }
}
