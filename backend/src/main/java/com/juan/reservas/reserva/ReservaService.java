package com.juan.reservas.reserva;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

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
    private final Clock clock;

    public ReservaService(NegocioRepository negocioRepository, ServicioRepository servicioRepository,
            RecursoRepository recursoRepository, ClienteRepository clienteRepository,
            ReservaRepository repository, DisponibilidadService disponibilidadService, Clock clock) {
        this.negocioRepository = negocioRepository;
        this.servicioRepository = servicioRepository;
        this.recursoRepository = recursoRepository;
        this.clienteRepository = clienteRepository;
        this.repository = repository;
        this.disponibilidadService = disponibilidadService;
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

        // 4. Cliente: el email lo identifica dentro del negocio
        Cliente cliente = buscarOCrearCliente(negocio, peticion, ahora);

        // 5. La reserva, con su bloqueo (fin + margen) y el hash del token del enlace
        Instant fin = peticion.inicio().plus(Duration.ofMinutes(servicio.getDuracionMin()));
        Instant finBloqueo = fin.plus(Duration.ofMinutes(servicio.getMargenMin()));
        EstadoReserva estado = negocio.isConfirmacionAutomatica() ? EstadoReserva.CONFIRMADA : EstadoReserva.PENDIENTE;
        String token = TokenSecreto.generar();
        Reserva reserva = new Reserva(negocio, servicio, recurso, cliente, peticion.inicio(), fin, finBloqueo,
                estado, textoOpcional(peticion.notas()), TokenSecreto.hash(token), ahora);

        // 6. saveAndFlush ejecuta el INSERT ya, dentro del método. Si la base de datos lo rechaza
        //    por reserva_sin_solape, la excepción sale de aquí y se traduce a 409, en vez de
        //    aparecer más tarde al confirmar la transacción
        repository.saveAndFlush(reserva);
        return new ReservaCreadaDto(token, aDto(reserva));
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

    private Cliente buscarOCrearCliente(Negocio negocio, ReservaPublicaRequest peticion, Instant ahora) {
        String email = peticion.email().trim().toLowerCase(Locale.ROOT);
        String nombre = peticion.nombre().trim();
        String telefono = textoOpcional(peticion.telefono());
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
