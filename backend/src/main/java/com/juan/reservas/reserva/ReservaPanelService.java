package com.juan.reservas.reserva;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.auth.Usuario;
import com.juan.reservas.auth.UsuarioActual;
import com.juan.reservas.auth.UsuarioRepository;
import com.juan.reservas.comun.NoEncontradoException;
import com.juan.reservas.comun.PeticionIncorrectaException;
import com.juan.reservas.negocio.Negocio;
import com.juan.reservas.negocio.NegocioRepository;
import com.juan.reservas.recurso.Recurso;
import com.juan.reservas.recurso.RecursoRepository;
import com.juan.reservas.reserva.ReservaService.DatosCliente;
import com.juan.reservas.reserva.dto.ReservaManualRequest;
import com.juan.reservas.reserva.dto.ReservaPanelDto;
import com.juan.reservas.servicio.Servicio;
import com.juan.reservas.servicio.ServicioRepository;

/**
 * Agenda del panel. Reglas de acceso:
 * <ul>
 *   <li>El propietario ve y gestiona todas las reservas de su negocio.</li>
 *   <li>Un empleado solo las de su propio recurso. Lo de otros recursos responde 404, igual
 *       que lo de otros negocios: no se revela que existe.</li>
 * </ul>
 * El recurso del empleado se lee de la base de datos en cada petición (no del JWT), así un
 * cambio de asignación se aplica al momento.
 */
@Service
public class ReservaPanelService {

    /** Límite del rango de la agenda: una vista mensual con margen. Evita consultas enormes. */
    static final Duration RANGO_MAXIMO = Duration.ofDays(62);

    private final ReservaRepository repository;
    private final ReservaService reservaService;
    private final NegocioRepository negocioRepository;
    private final ServicioRepository servicioRepository;
    private final RecursoRepository recursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public ReservaPanelService(ReservaRepository repository, ReservaService reservaService,
            NegocioRepository negocioRepository, ServicioRepository servicioRepository,
            RecursoRepository recursoRepository, UsuarioRepository usuarioRepository,
            ApplicationEventPublisher eventos, Clock clock) {
        this.repository = repository;
        this.reservaService = reservaService;
        this.negocioRepository = negocioRepository;
        this.servicioRepository = servicioRepository;
        this.recursoRepository = recursoRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventos = eventos;
        this.clock = clock;
    }

    /**
     * Reservas que se cruzan con [desde, hasta). El propietario puede filtrar por recurso;
     * a un empleado siempre se le muestra solo el suyo.
     */
    @Transactional(readOnly = true)
    public List<ReservaPanelDto> agenda(UsuarioActual usuario, Instant desde, Instant hasta, UUID recursoId) {
        if (!desde.isBefore(hasta)) {
            throw new PeticionIncorrectaException("'desde' debe ser anterior a 'hasta'");
        }
        if (Duration.between(desde, hasta).compareTo(RANGO_MAXIMO) > 0) {
            throw new PeticionIncorrectaException("El rango de la agenda no puede superar los 62 días");
        }

        UUID filtro = usuario.esPropietario() ? recursoId : recursoDelEmpleado(usuario);
        if (!usuario.esPropietario() && filtro == null) {
            return List.of(); // empleado sin recurso asignado: no tiene agenda
        }
        List<Reserva> reservas = filtro == null
                ? repository.findAgenda(usuario.negocioId(), desde, hasta)
                : repository.findAgendaDeRecurso(usuario.negocioId(), filtro, desde, hasta);
        return reservas.stream().map(ReservaPanelService::aDto).toList();
    }

    /** Reserva por teléfono: siempre confirmada y sin reglas de antelación ni de horario. */
    @Transactional
    public ReservaPanelDto crearManual(UsuarioActual usuario, ReservaManualRequest peticion) {
        Negocio negocio = negocioRepository.findById(usuario.negocioId())
                .orElseThrow(() -> new NoEncontradoException("Negocio no encontrado"));
        Servicio servicio = servicioRepository.findByIdAndNegocioId(peticion.servicioId(), negocio.getId())
                .filter(Servicio::isActivo)
                .orElseThrow(() -> new NoEncontradoException("Servicio no encontrado"));
        Recurso recurso = recursoRepository.findByIdAndNegocioId(peticion.recursoId(), negocio.getId())
                .filter(Recurso::isActivo)
                .filter(r -> r.servicioIds().contains(servicio.getId()))
                .filter(r -> usuario.esPropietario() || r.getId().equals(recursoDelEmpleado(usuario)))
                .orElseThrow(() -> new NoEncontradoException("Recurso no encontrado"));

        var datos = new DatosCliente(peticion.nombre(), peticion.email(), peticion.telefono(), peticion.notas());
        Reserva reserva = reservaService.registrar(negocio, servicio, recurso, peticion.inicio(), datos,
                EstadoReserva.CONFIRMADA, Instant.now(clock)).reserva();
        return aDto(reserva);
    }

    /**
     * Confirmar, cancelar, completar o marcar no presentado (422 si el cambio no está permitido).
     * Si el negocio cancela una reserva, el cliente recibe un email.
     */
    @Transactional
    public ReservaPanelDto cambiarEstado(UsuarioActual usuario, UUID reservaId, EstadoReserva nuevo) {
        Reserva reserva = repository.findParaCambiar(reservaId, usuario.negocioId())
                .filter(r -> usuario.esPropietario() || r.getRecurso().getId().equals(recursoDelEmpleado(usuario)))
                .orElseThrow(() -> new NoEncontradoException("Reserva no encontrada"));
        reserva.cambiarEstado(nuevo);
        if (nuevo == EstadoReserva.CANCELADA) {
            eventos.publishEvent(new ReservaCancelada(reserva.getId(), false));
        }
        return aDto(reserva);
    }

    private UUID recursoDelEmpleado(UsuarioActual usuario) {
        return usuarioRepository.findById(usuario.usuarioId())
                .map(Usuario::getRecursoId)
                .orElse(null);
    }

    private static ReservaPanelDto aDto(Reserva reserva) {
        var cliente = reserva.getCliente();
        return new ReservaPanelDto(
                reserva.getId(),
                reserva.getEstado(),
                reserva.getInicio(),
                reserva.getFin(),
                reserva.getFinBloqueo(),
                reserva.getNotas(),
                new ReservaPanelDto.Servicio(reserva.getServicio().getId(), reserva.getServicio().getNombre(),
                        reserva.getServicio().getPrecioCentimos()),
                new ReservaPanelDto.Recurso(reserva.getRecurso().getId(), reserva.getRecurso().getNombre()),
                new ReservaPanelDto.Cliente(cliente.getNombre(), cliente.getEmail(), cliente.getTelefono()));
    }
}
