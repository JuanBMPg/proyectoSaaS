package com.juan.reservas.recurso;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.comun.NoEncontradoException;
import com.juan.reservas.negocio.NegocioRepository;
import com.juan.reservas.recurso.dto.AsignacionServiciosRequest;
import com.juan.reservas.recurso.dto.RecursoDto;
import com.juan.reservas.recurso.dto.RecursoRequest;
import com.juan.reservas.servicio.Servicio;
import com.juan.reservas.servicio.ServicioRepository;

/** Gestión de los recursos de un negocio y de los servicios que presta cada uno. */
@Service
public class RecursoService {

    private final RecursoRepository repository;
    private final ServicioRepository servicioRepository;
    private final NegocioRepository negocioRepository;
    private final RecursoMapper mapper;

    public RecursoService(RecursoRepository repository, ServicioRepository servicioRepository,
            NegocioRepository negocioRepository, RecursoMapper mapper) {
        this.repository = repository;
        this.servicioRepository = servicioRepository;
        this.negocioRepository = negocioRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<RecursoDto> listar(UUID negocioId) {
        return repository.findAllByNegocioIdOrderByNombre(negocioId).stream().map(mapper::aDto).toList();
    }

    @Transactional
    public RecursoDto crear(UUID negocioId, RecursoRequest peticion) {
        Recurso recurso = new Recurso(negocioRepository.getReferenceById(negocioId),
                peticion.nombre().trim(), peticion.tipo());
        if (peticion.activo() != null) {
            recurso.cambiarActivo(peticion.activo());
        }
        return mapper.aDto(repository.save(recurso));
    }

    @Transactional
    public RecursoDto actualizar(UUID negocioId, UUID id, RecursoRequest peticion) {
        Recurso recurso = buscar(negocioId, id);
        recurso.actualizar(peticion.nombre().trim(), peticion.tipo());
        if (peticion.activo() != null) {
            recurso.cambiarActivo(peticion.activo());
        }
        return mapper.aDto(recurso);
    }

    /** Idempotente: desactivar un recurso ya desactivado no es un error. */
    @Transactional
    public void desactivar(UUID negocioId, UUID id) {
        buscar(negocioId, id).desactivar();
    }

    /**
     * Sustituye el conjunto de servicios que presta el recurso.
     * Si algún servicio no existe o es de otro negocio, responde 404 y no cambia nada.
     */
    @Transactional
    public RecursoDto asignarServicios(UUID negocioId, UUID recursoId, AsignacionServiciosRequest peticion) {
        Recurso recurso = buscar(negocioId, recursoId);
        List<Servicio> servicios = servicioRepository.findAllByIdInAndNegocioId(peticion.servicioIds(), negocioId);
        if (servicios.size() != peticion.servicioIds().size()) {
            throw new NoEncontradoException("Servicio no encontrado");
        }
        recurso.asignarServicios(servicios);
        return mapper.aDto(recurso);
    }

    private Recurso buscar(UUID negocioId, UUID id) {
        return repository.findByIdAndNegocioId(id, negocioId)
                .orElseThrow(() -> new NoEncontradoException("Recurso no encontrado"));
    }
}
