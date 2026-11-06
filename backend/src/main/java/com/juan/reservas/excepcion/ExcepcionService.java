package com.juan.reservas.excepcion;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.comun.NoEncontradoException;
import com.juan.reservas.excepcion.dto.ExcepcionCreadaDto;
import com.juan.reservas.excepcion.dto.ExcepcionDto;
import com.juan.reservas.excepcion.dto.ExcepcionRequest;
import com.juan.reservas.negocio.Negocio;
import com.juan.reservas.negocio.NegocioRepository;
import com.juan.reservas.recurso.Recurso;
import com.juan.reservas.recurso.RecursoRepository;
import com.juan.reservas.reserva.EstadoReserva;
import com.juan.reservas.reserva.ReservaRepository;

/**
 * Vacaciones, festivos y horarios especiales. La disponibilidad las tiene en cuenta en
 * {@link com.juan.reservas.disponibilidad.DisponibilidadService}: aquí solo se gestionan.
 * <p>
 * No hay edición: una excepción se borra y se crea otra. Son datos pequeños y así no hay que
 * decidir qué pasa con una excepción que se mueve de fechas.
 */
@Service
public class ExcepcionService {

    private final ExcepcionRepository repository;
    private final NegocioRepository negocioRepository;
    private final RecursoRepository recursoRepository;
    private final ReservaRepository reservaRepository;
    private final Clock clock;

    public ExcepcionService(ExcepcionRepository repository, NegocioRepository negocioRepository,
            RecursoRepository recursoRepository, ReservaRepository reservaRepository, Clock clock) {
        this.repository = repository;
        this.negocioRepository = negocioRepository;
        this.recursoRepository = recursoRepository;
        this.reservaRepository = reservaRepository;
        this.clock = clock;
    }

    /**
     * Las que siguen vigentes en {@code desde} o empiezan después. Sin {@code desde}, las de
     * hoy en adelante (hoy en la zona del negocio, no en la del servidor).
     */
    @Transactional(readOnly = true)
    public List<ExcepcionDto> listar(UUID negocioId, LocalDate desde) {
        Negocio negocio = negocio(negocioId);
        LocalDate inicio = desde != null ? desde : LocalDate.now(clock.withZone(ZoneId.of(negocio.getZonaHoraria())));
        return repository.findVigentes(negocioId, inicio).stream().map(ExcepcionService::aDto).toList();
    }

    @Transactional
    public ExcepcionCreadaDto crear(UUID negocioId, ExcepcionRequest peticion) {
        Negocio negocio = negocio(negocioId);
        // Un recurso de otro negocio da 404, como si no existiera
        Recurso recurso = peticion.recursoId() == null ? null
                : recursoRepository.findByIdAndNegocioId(peticion.recursoId(), negocioId)
                        .orElseThrow(() -> new NoEncontradoException("Recurso no encontrado"));

        Excepcion excepcion = repository.save(new Excepcion(negocio, recurso, peticion.fechaInicio(),
                peticion.fechaFin(), peticion.horaInicio(), peticion.horaFin(), peticion.motivo().trim(),
                Instant.now(clock)));
        return new ExcepcionCreadaDto(aDto(excepcion), reservasActivas(negocio, excepcion));
    }

    @Transactional
    public void borrar(UUID negocioId, UUID id) {
        Excepcion excepcion = repository.findByIdAndNegocioId(id, negocioId)
                .orElseThrow(() -> new NoEncontradoException("Excepción no encontrada"));
        repository.delete(excepcion);
    }

    /** Reservas activas de los recursos afectados que empiezan en esos días (en la zona del negocio). */
    private long reservasActivas(Negocio negocio, Excepcion excepcion) {
        ZoneId zona = ZoneId.of(negocio.getZonaHoraria());
        Instant desde = excepcion.getFechaInicio().atStartOfDay(zona).toInstant();
        Instant hasta = excepcion.getFechaFin().plusDays(1).atStartOfDay(zona).toInstant();
        Recurso recurso = excepcion.getRecurso();
        return recurso == null
                ? reservaRepository.contarEnRango(negocio.getId(), EstadoReserva.ACTIVOS, desde, hasta)
                : reservaRepository.contarEnRangoDeRecurso(negocio.getId(), recurso.getId(), EstadoReserva.ACTIVOS,
                        desde, hasta);
    }

    private Negocio negocio(UUID negocioId) {
        return negocioRepository.findById(negocioId)
                .orElseThrow(() -> new NoEncontradoException("Negocio no encontrado"));
    }

    private static ExcepcionDto aDto(Excepcion excepcion) {
        Recurso recurso = excepcion.getRecurso();
        return new ExcepcionDto(
                excepcion.getId(),
                recurso == null ? null : new ExcepcionDto.RecursoAfectado(recurso.getId(), recurso.getNombre()),
                excepcion.getFechaInicio(),
                excepcion.getFechaFin(),
                excepcion.getHoraInicio(),
                excepcion.getHoraFin(),
                excepcion.getMotivo());
    }
}
