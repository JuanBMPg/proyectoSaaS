package com.juan.reservas.notificacion;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.juan.reservas.comun.NoEncontradoException;
import com.juan.reservas.reserva.ReservaCreada;
import com.juan.reservas.reserva.ReservaRepository;

/**
 * Deja en la cola el email de una reserva recién creada.
 * <p>
 * Es un {@code @EventListener} normal: se ejecuta en el mismo hilo y DENTRO de la transacción
 * que crea la reserva. Si la reserva se guarda, su email también; si hay un rollback (por
 * ejemplo, 409 por solape), desaparecen los dos. El envío real lo hace {@link EnviadorEmails}.
 * <p>
 * El evento evita que el paquete reserva dependa del de notificaciones.
 */
@Component
public class EmailReservaListener {

    private final NotificadorReservas notificador;
    private final ReservaRepository reservaRepository;

    public EmailReservaListener(NotificadorReservas notificador, ReservaRepository reservaRepository) {
        this.notificador = notificador;
        this.reservaRepository = reservaRepository;
    }

    @EventListener
    public void alCrearReserva(ReservaCreada evento) {
        // Dentro de la misma transacción: la reserva ya está en el contexto de persistencia
        var reserva = reservaRepository.findById(evento.reservaId())
                .orElseThrow(() -> new NoEncontradoException("Reserva no encontrada: " + evento.reservaId()));
        notificador.reservaCreada(reserva, evento.token());
    }
}
