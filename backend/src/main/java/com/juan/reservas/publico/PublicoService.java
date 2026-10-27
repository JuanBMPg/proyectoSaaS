package com.juan.reservas.publico;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.comun.NoEncontradoException;
import com.juan.reservas.disponibilidad.DisponibilidadService;
import com.juan.reservas.negocio.Negocio;
import com.juan.reservas.negocio.NegocioRepository;
import com.juan.reservas.publico.dto.DisponibilidadDto;
import com.juan.reservas.publico.dto.DisponibilidadDto.HuecoDto;
import com.juan.reservas.publico.dto.NegocioPublicoDto;
import com.juan.reservas.publico.dto.RecursoPublicoDto;
import com.juan.reservas.publico.dto.ServicioPublicoDto;
import com.juan.reservas.recurso.Recurso;
import com.juan.reservas.recurso.RecursoRepository;
import com.juan.reservas.servicio.Servicio;
import com.juan.reservas.servicio.ServicioRepository;

/**
 * Datos de la web pública. Identifica el negocio por su slug y solo muestra lo reservable:
 * servicios activos que presta al menos un recurso activo.
 */
@Service
@Transactional(readOnly = true)
public class PublicoService {

    private final NegocioRepository negocioRepository;
    private final ServicioRepository servicioRepository;
    private final RecursoRepository recursoRepository;
    private final DisponibilidadService disponibilidadService;

    public PublicoService(NegocioRepository negocioRepository, ServicioRepository servicioRepository,
            RecursoRepository recursoRepository, DisponibilidadService disponibilidadService) {
        this.negocioRepository = negocioRepository;
        this.servicioRepository = servicioRepository;
        this.recursoRepository = recursoRepository;
        this.disponibilidadService = disponibilidadService;
    }

    public NegocioPublicoDto negocio(String slug) {
        Negocio negocio = buscarNegocio(slug);
        List<Recurso> recursos = recursosActivos(negocio);

        List<ServicioPublicoDto> servicios = servicioRepository.findAllByNegocioIdOrderByNombre(negocio.getId())
                .stream()
                .filter(Servicio::isActivo)
                .map(servicio -> aDto(servicio, recursos))
                .filter(servicio -> !servicio.recursoIds().isEmpty())
                .toList();

        // Solo los recursos que prestan algún servicio visible
        var recursosVisibles = servicios.stream()
                .flatMap(servicio -> servicio.recursoIds().stream())
                .collect(Collectors.toSet());
        List<RecursoPublicoDto> recursosDto = recursos.stream()
                .filter(recurso -> recursosVisibles.contains(recurso.getId()))
                .map(recurso -> new RecursoPublicoDto(recurso.getId(), recurso.getNombre()))
                .toList();

        return new NegocioPublicoDto(negocio.getNombre(), negocio.getSlug(), negocio.getZonaHoraria(),
                negocio.getDireccion(), negocio.getTelefono(), negocio.getEtiquetaRecurso(),
                negocio.getAntelacionMaximaDias(), servicios, recursosDto);
    }

    /**
     * Huecos de un día para un servicio, con todos los recursos que lo prestan o con uno concreto.
     */
    public DisponibilidadDto disponibilidad(String slug, UUID servicioId, LocalDate fecha, UUID recursoId) {
        Negocio negocio = buscarNegocio(slug);
        Servicio servicio = servicioRepository.findByIdAndNegocioId(servicioId, negocio.getId())
                .filter(Servicio::isActivo)
                .orElseThrow(() -> new NoEncontradoException("Servicio no encontrado"));

        List<UUID> recursoIds = recursosActivos(negocio).stream()
                .filter(recurso -> recurso.servicioIds().contains(servicioId))
                .filter(recurso -> recursoId == null || recurso.getId().equals(recursoId))
                .map(Recurso::getId)
                .toList();
        if (recursoId != null && recursoIds.isEmpty()) {
            throw new NoEncontradoException("Recurso no encontrado");
        }

        List<HuecoDto> huecos = disponibilidadService.huecos(negocio, servicio, fecha, recursoIds).stream()
                .map(hueco -> new HuecoDto(hueco.recursoId(), hueco.inicio(), hueco.fin()))
                .toList();
        return new DisponibilidadDto(fecha, negocio.getZonaHoraria(), huecos);
    }

    private Negocio buscarNegocio(String slug) {
        return negocioRepository.findBySlug(slug)
                .orElseThrow(() -> new NoEncontradoException("Negocio no encontrado"));
    }

    /** Recursos activos con sus asignaciones ya cargadas (una consulta, ver RecursoRepository). */
    private List<Recurso> recursosActivos(Negocio negocio) {
        return recursoRepository.findAllByNegocioIdOrderByNombre(negocio.getId()).stream()
                .filter(Recurso::isActivo)
                .toList();
    }

    private static ServicioPublicoDto aDto(Servicio servicio, List<Recurso> recursos) {
        List<UUID> quienLoPresta = recursos.stream()
                .filter(recurso -> recurso.servicioIds().contains(servicio.getId()))
                .sorted(Comparator.comparing(Recurso::getNombre))
                .map(Recurso::getId)
                .toList();
        return new ServicioPublicoDto(servicio.getId(), servicio.getNombre(), servicio.getDescripcion(),
                servicio.getDuracionMin(), servicio.getPrecioCentimos(), quienLoPresta);
    }
}
