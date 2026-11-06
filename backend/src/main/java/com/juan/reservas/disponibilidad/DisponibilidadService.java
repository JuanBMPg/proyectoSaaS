package com.juan.reservas.disponibilidad;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.excepcion.Excepcion;
import com.juan.reservas.excepcion.ExcepcionRepository;
import com.juan.reservas.negocio.Negocio;
import com.juan.reservas.recurso.Horario;
import com.juan.reservas.recurso.HorarioRepository;
import com.juan.reservas.reserva.EstadoReserva;
import com.juan.reservas.reserva.Reserva;
import com.juan.reservas.reserva.ReservaRepository;
import com.juan.reservas.servicio.Servicio;

/**
 * Carga de la base de datos lo que necesita la {@link CalculadoraDisponibilidad} y la llama.
 * <p>
 * Lo usan la web pública, para ofrecer huecos, y la creación de reservas, para volver a
 * comprobar dentro de la transacción que el hueco sigue libre: los dos ven exactamente lo mismo.
 */
@Service
public class DisponibilidadService {

    private final HorarioRepository horarioRepository;
    private final ReservaRepository reservaRepository;
    private final ExcepcionRepository excepcionRepository;
    private final CalculadoraDisponibilidad calculadora;
    private final Clock clock;

    public DisponibilidadService(HorarioRepository horarioRepository, ReservaRepository reservaRepository,
            ExcepcionRepository excepcionRepository, CalculadoraDisponibilidad calculadora, Clock clock) {
        this.horarioRepository = horarioRepository;
        this.reservaRepository = reservaRepository;
        this.excepcionRepository = excepcionRepository;
        this.calculadora = calculadora;
        this.clock = clock;
    }

    /**
     * Huecos de un día para un servicio con los recursos indicados (que ya deben prestarlo).
     * Franjas, excepciones y reservas se cargan con una consulta cada una, sin importar cuántos
     * recursos haya. Las excepciones del día (vacaciones, festivos, horarios especiales) cambian
     * las franjas antes de llamar a la calculadora (ver {@link HorarioDelDia}).
     */
    @Transactional(readOnly = true)
    public List<Hueco> huecos(Negocio negocio, Servicio servicio, LocalDate fecha, List<UUID> recursoIds) {
        if (recursoIds.isEmpty()) {
            return List.of();
        }
        ZoneId zona = ZoneId.of(negocio.getZonaHoraria());
        Instant inicioDia = fecha.atStartOfDay(zona).toInstant();
        Instant finDia = fecha.plusDays(1).atStartOfDay(zona).toInstant();

        Map<UUID, List<FranjaLocal>> franjas = horarioRepository
                .findAllByRecursoIdInAndDiaSemana(recursoIds, fecha.getDayOfWeek()).stream()
                .collect(Collectors.groupingBy(Horario::getRecursoId,
                        Collectors.mapping(h -> new FranjaLocal(h.getHoraInicio(), h.getHoraFin()),
                                Collectors.toList())));
        Map<UUID, List<Intervalo>> ocupados = reservaRepository
                .findQueOcupan(recursoIds, EstadoReserva.ACTIVOS, inicioDia, finDia).stream()
                .collect(Collectors.groupingBy(reserva -> reserva.getRecurso().getId(),
                        Collectors.mapping(DisponibilidadService::aIntervalo, Collectors.toList())));

        List<Excepcion> excepciones = excepcionRepository.findDelDia(negocio.getId(), fecha, recursoIds);

        List<AgendaRecurso> agendas = recursoIds.stream()
                .map(id -> new AgendaRecurso(id,
                        HorarioDelDia.franjas(franjas.getOrDefault(id, List.of()), excepcionesDe(id, excepciones)),
                        ocupados.getOrDefault(id, List.of())))
                .toList();

        return calculadora.calcular(new SolicitudDisponibilidad(fecha, zona,
                Duration.ofMinutes(servicio.getDuracionMin()), Duration.ofMinutes(servicio.getMargenMin()),
                Instant.now(clock), Duration.ofMinutes(negocio.getAntelacionMinimaMin()),
                negocio.getAntelacionMaximaDias(), SolicitudDisponibilidad.PASO_POR_DEFECTO, agendas));
    }

    private static List<ExcepcionDelDia> excepcionesDe(UUID recursoId, List<Excepcion> excepciones) {
        return excepciones.stream()
                .filter(excepcion -> excepcion.afectaA(recursoId))
                .map(DisponibilidadService::aExcepcionDelDia)
                .toList();
    }

    /** La vista que necesita la lógica pura, sin JPA. */
    private static ExcepcionDelDia aExcepcionDelDia(Excepcion excepcion) {
        boolean deTodoElNegocio = excepcion.getRecurso() == null;
        return excepcion.esCierre()
                ? ExcepcionDelDia.cierre(deTodoElNegocio)
                : ExcepcionDelDia.horarioEspecial(deTodoElNegocio,
                        new FranjaLocal(excepcion.getHoraInicio(), excepcion.getHoraFin()));
    }

    private static Intervalo aIntervalo(Reserva reserva) {
        return new Intervalo(reserva.getInicio(), reserva.getFinBloqueo());
    }
}
