package com.juan.reservas.reserva;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.cliente.Cliente;
import com.juan.reservas.cliente.ClienteRepository;
import com.juan.reservas.comun.ConflictoException;
import com.juan.reservas.comun.NoEncontradoException;
import com.juan.reservas.comun.ReglaNegocioException;
import com.juan.reservas.comun.TokenSecreto;
import com.juan.reservas.disponibilidad.DisponibilidadService;
import com.juan.reservas.negocio.Negocio;
import com.juan.reservas.negocio.NegocioRepository;
import com.juan.reservas.recurso.Recurso;
import com.juan.reservas.recurso.RecursoRepository;
import com.juan.reservas.reserva.dto.ReservaCreadaDto;
import com.juan.reservas.reserva.dto.ReservaPublicaDto;
import com.juan.reservas.reserva.dto.ReservaPublicaRequest;
import com.juan.reservas.servicio.Servicio;
import com.juan.reservas.servicio.ServicioRepository;

/**
 * Reservas hechas por los clientes desde la web pública.
 * <p>
 * Dos capas contra las dobles reservas:
 * <ol>
 *   <li>Dentro de la transacción se vuelve a calcular la disponibilidad y se exige que el hueco
 *       siga libre. Cubre el caso normal y rechaza horas inventadas (fuera de horario o de la
 *       rejilla de 15 min) de quien llame a la API sin pasar por la página.</li>
 *   <li>Dos peticiones simultáneas pueden pasar las dos esa comprobación. La garantía real es
 *       la restricción reserva_sin_solape de PostgreSQL: el INSERT de la segunda falla con
 *       SQLState 23P01 y el manejador global responde 409.</li>
 * </ol>
 */
@Service
public class ReservaService {

    static final String HUECO_OCUPADO = "Ese hueco acaba de ocuparse";

    private final NegocioRepository negocioRepository;
    private final ServicioRepository servicioRepository;
    private final RecursoRepository recursoRepository;
    private final ClienteRepository clienteRepository;
    private final ReservaRepository repository;
    private final DisponibilidadService disponibilidadService;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public ReservaService(NegocioRepository negocioRepository, ServicioRepository servicioRepository,
            RecursoRepository recursoRepository, ClienteRepository clienteRepository,
            ReservaRepository repository, DisponibilidadService disponibilidadService,
            ApplicationEventPublisher eventos, Clock clock) {
        this.negocioRepository = negocioRepository;
        this.servicioRepository = servicioRepository;
        this.recursoRepository = recursoRepository;
        this.clienteRepository = clienteRepository;
        this.repository = repository;
        this.disponibilidadService = disponibilidadService;
        this.eventos = eventos;
        this.clock = clock;
    }

    @Transactional
    public ReservaCreadaDto crearDesdeWeb(String slug, ReservaPublicaRequest peticion) {
        // 1. Negocio, servicio y recurso: tienen que existir y corresponderse (si no, 404)
        Negocio negocio = negocioRepository.findBySlug(slug)
                .orElseThrow(() -> new NoEncontradoException("Negocio no encontrado"));
        Servicio servicio = servicioRepository.findByIdAndNegocioId(peticion.servicioId(), negocio.getId())
                .filter(Servicio::isActivo)
                .orElseThrow(() -> new NoEncontradoException("Servicio no encontrado"));
        Recurso recurso = recursoRepository.findByIdAndNegocioId(peticion.recursoId(), negocio.getId())
                .filter(Recurso::isActivo)
                .filter(r -> r.servicioIds().contains(servicio.getId()))
                .orElseThrow(() -> new NoEncontradoException("Recurso no encontrado"));

        // 2. Reglas de antelación: son reglas del negocio (422), no un hueco ocupado
        Instant ahora = Instant.now(clock);
        ZoneId zona = ZoneId.of(negocio.getZonaHoraria());
        comprobarAntelacion(peticion.inicio(), ahora, zona, negocio);

        // 3. El hueco tiene que seguir libre según la misma calculadora que muestra la página
        boolean libre = disponibilidadService
                .huecos(negocio, servicio, peticion.inicio().atZone(zona).toLocalDate(), List.of(recurso.getId()))
                .stream()
                .anyMatch(hueco -> hueco.inicio().equals(peticion.inicio()));
        if (!libre) {
            throw new ConflictoException(HUECO_OCUPADO);
        }

        // 4. Cliente, reserva, INSERT y aviso del email (compartido con las reservas del panel)
        EstadoReserva estado = negocio.isConfirmacionAutomatica() ? EstadoReserva.CONFIRMADA : EstadoReserva.PENDIENTE;
        var datos = new DatosCliente(peticion.nombre(), peticion.email(), peticion.telefono(), peticion.notas());
        ReservaRegistrada registrada = registrar(negocio, servicio, recurso, peticion.inicio(), datos, estado, ahora);
        return new ReservaCreadaDto(registrada.token(), aDto(registrada.reserva()));
    }

    /** Datos del cliente tal como llegan en la petición (sin normalizar). */
    record DatosCliente(String nombre, String email, String telefono, String notas) {
    }

    /** La reserva guardada y el token en claro de su enlace, que no se guarda en la BD. */
    record ReservaRegistrada(Reserva reserva, String token) {
    }

    /**
     * Guarda una reserva ya validada por quien llama: la web pública (con sus reglas de
     * antelación y disponibilidad) o el panel del negocio (que puede reservar a cualquier hora).
     * En los dos casos la restricción reserva_sin_solape impide el solape (409).
     */
    ReservaRegistrada registrar(Negocio negocio, Servicio servicio, Recurso recurso, Instant inicio,
            DatosCliente datos, EstadoReserva estado, Instant ahora) {
        // El email identifica al cliente dentro del negocio
        Cliente cliente = buscarOCrearCliente(negocio, datos, ahora);

        // La reserva, con su bloqueo (fin + margen) y el hash del token del enlace
        Instant fin = inicio.plus(Duration.ofMinutes(servicio.getDuracionMin()));
        Instant finBloqueo = fin.plus(Duration.ofMinutes(servicio.getMargenMin()));
        String token = TokenSecreto.generar();
        Reserva reserva = new Reserva(negocio, servicio, recurso, cliente, inicio, fin, finBloqueo,
                estado, textoOpcional(datos.notas()), TokenSecreto.hash(token), ahora);

        // saveAndFlush ejecuta el INSERT ya, dentro del método. Si la base de datos lo rechaza
        // por reserva_sin_solape, la excepción sale de aquí y se traduce a 409, en vez de
        // aparecer más tarde al confirmar la transacción
        repository.saveAndFlush(reserva);

        // Aviso para el email de confirmación: el listener solo actúa si la transacción se confirma
        eventos.publishEvent(new ReservaCreada(reserva.getId(), token));
        return new ReservaRegistrada(reserva, token);
    }

    @Transactional(readOnly = true)
    public ReservaPublicaDto buscarPorToken(String token) {
        return repository.findByTokenHash(TokenSecreto.hash(token))
                .map(ReservaService::aDto)
                .orElseThrow(() -> new NoEncontradoException("Esta reserva no existe"));
    }

    private static void comprobarAntelacion(Instant inicio, Instant ahora, ZoneId zona, Negocio negocio) {
        Duration minima = Duration.ofMinutes(negocio.getAntelacionMinimaMin());
        if (inicio.isBefore(ahora.plus(minima))) {
            throw new ReglaNegocioException("Las reservas se hacen con al menos " + describir(minima)
                    + " de antelación");
        }
        Instant ultimoInicio = ahora.atZone(zona).plusDays(negocio.getAntelacionMaximaDias()).toInstant();
        if (inicio.isAfter(ultimoInicio)) {
            throw new ReglaNegocioException("Solo se puede reservar con hasta "
                    + negocio.getAntelacionMaximaDias() + " días de antelación");
        }
    }

    /** "2 horas", "1 hora", "45 minutos". */
    private static String describir(Duration duracion) {
        long minutos = duracion.toMinutes();
        if (minutos % 60 == 0) {
            long horas = minutos / 60;
            return horas == 1 ? "1 hora" : horas + " horas";
        }
        return minutos + " minutos";
    }

    private Cliente buscarOCrearCliente(Negocio negocio, DatosCliente datos, Instant ahora) {
        String email = datos.email().trim().toLowerCase(Locale.ROOT);
        String nombre = datos.nombre().trim();
        String telefono = textoOpcional(datos.telefono());
        return clienteRepository.findByNegocioIdAndEmail(negocio.getId(), email)
                .map(cliente -> {
                    cliente.actualizarContacto(nombre, telefono);
                    return cliente;
                })
                .orElseGet(() -> clienteRepository.save(new Cliente(negocio, nombre, email, telefono, ahora)));
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static ReservaPublicaDto aDto(Reserva reserva) {
        Negocio negocio = reserva.getNegocio();
        Servicio servicio = reserva.getServicio();
        return new ReservaPublicaDto(
                reserva.getEstado(),
                reserva.getInicio(),
                reserva.getFin(),
                reserva.getCliente().getNombre(),
                new ReservaPublicaDto.Servicio(servicio.getNombre(), servicio.getDuracionMin(),
                        servicio.getPrecioCentimos()),
                new ReservaPublicaDto.Recurso(reserva.getRecurso().getNombre(), negocio.getEtiquetaRecurso()),
                new ReservaPublicaDto.Negocio(negocio.getNombre(), negocio.getSlug(), negocio.getZonaHoraria(),
                        negocio.getDireccion(), negocio.getTelefono()));
    }
}
