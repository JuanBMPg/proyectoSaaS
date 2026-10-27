package com.juan.reservas.servicio;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.comun.NoEncontradoException;
import com.juan.reservas.negocio.NegocioRepository;
import com.juan.reservas.servicio.dto.ServicioDto;
import com.juan.reservas.servicio.dto.ServicioRequest;

/** Gestión de los servicios de un negocio. Todos los métodos reciben el negocio del token. */
@Service
public class ServicioService {

    private final ServicioRepository repository;
    private final NegocioRepository negocioRepository;
    private final ServicioMapper mapper;

    public ServicioService(ServicioRepository repository, NegocioRepository negocioRepository,
            ServicioMapper mapper) {
        this.repository = repository;
        this.negocioRepository = negocioRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ServicioDto> listar(UUID negocioId) {
        return repository.findAllByNegocioIdOrderByNombre(negocioId).stream().map(mapper::aDto).toList();
    }

    @Transactional
    public ServicioDto crear(UUID negocioId, ServicioRequest peticion) {
        // getReferenceById no consulta la BD: basta con la referencia para rellenar negocio_id
        Servicio servicio = new Servicio(negocioRepository.getReferenceById(negocioId),
                peticion.nombre().trim(), peticion.descripcion(),
                peticion.duracionMin(), peticion.margenMin(), peticion.precioCentimos());
        if (peticion.activo() != null) {
            servicio.cambiarActivo(peticion.activo());
        }
        return mapper.aDto(repository.save(servicio));
    }

    /** Sin save(): la entidad está gestionada y Hibernate guarda los cambios al confirmar. */
    @Transactional
    public ServicioDto actualizar(UUID negocioId, UUID id, ServicioRequest peticion) {
        Servicio servicio = buscar(negocioId, id);
        servicio.actualizar(peticion.nombre().trim(), peticion.descripcion(),
                peticion.duracionMin(), peticion.margenMin(), peticion.precioCentimos());
        if (peticion.activo() != null) {
            servicio.cambiarActivo(peticion.activo());
        }
        return mapper.aDto(servicio);
    }

    /** Idempotente: desactivar un servicio ya desactivado no es un error. */
    @Transactional
    public void desactivar(UUID negocioId, UUID id) {
        buscar(negocioId, id).desactivar();
    }

    private Servicio buscar(UUID negocioId, UUID id) {
        return repository.findByIdAndNegocioId(id, negocioId)
                .orElseThrow(() -> new NoEncontradoException("Servicio no encontrado"));
    }
}
