package com.juan.reservas.notificacion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.reserva.EstadoReserva;
import com.juan.reservas.reserva.Reserva;

/**
 * Pone en la cola el recordatorio de las citas confirmadas que empiezan en las próximas
 * 24 horas. Se ejecuta cada pocos minutos, así que cada cita recibe el suyo con unas 24 h
 * de antelación.
 * <p>
 * Reglas:
 * <ul>
 *   <li>Solo reservas CONFIRMADAS: una pendiente aún no es una cita en firme y una cancelada
 *       ya no existe.</li>
 *   <li>Si se reservó con menos de 24 h de antelación, no hay recordatorio: el email de
 *       confirmación es reciente.</li>
 *   <li>Uno por reserva: la consulta salta las que ya lo tienen y, si dos instancias lo
 *       intentaran a la vez, el índice único email_pendiente_un_recordatorio_uk rechazaría
 *       el segundo.</li>
 * </ul>
 */
@Component
public class ProgramadorRecordatorios {

    private static final Logger log = LoggerFactory.getLogger(ProgramadorRecordatorios.class);

    private final EmailPendienteRepository repository;
    private final NotificadorReservas notificador;
    private final Clock clock;
    private final Duration antelacion;

    public ProgramadorRecordatorios(EmailPendienteRepository repository, NotificadorReservas notificador,
            Clock clock, @Value("${app.recordatorios.antelacion:PT24H}") Duration antelacion) {
        this.repository = repository;
        this.notificador = notificador;
        this.clock = clock;
        this.antelacion = antelacion;
    }

    @Scheduled(fixedDelayString = "${app.recordatorios.intervalo:PT5M}", initialDelayString = "PT30S")
    public void programarAhora() {
        programar(Instant.now(clock));
    }

    /** Separado de la tarea programada para poder probarlo con cualquier "ahora". */
    @Transactional
    public int programar(Instant ahora) {
        List<Reserva> reservas = repository.findSinRecordatorio(EstadoReserva.CONFIRMADA, ahora, ahora.plus(antelacion))
                .stream()
                .filter(reserva -> !reserva.getCreadoEn().plus(antelacion).isAfter(reserva.getInicio()))
                .toList();
        reservas.forEach(notificador::recordatorio);
        if (!reservas.isEmpty()) {
            log.info("{} recordatorios en la cola", reservas.size());
        }
        return reservas.size();
    }
}
