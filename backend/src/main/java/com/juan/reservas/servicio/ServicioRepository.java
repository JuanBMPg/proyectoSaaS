package com.juan.reservas.servicio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Todas las consultas filtran por negocio: nunca se busca un servicio solo por su id.
 * Si pertenece a otro negocio, el resultado es el mismo que si no existiera.
 */
public interface ServicioRepository extends JpaRepository<Servicio, UUID> {

    List<Servicio> findAllByNegocioIdOrderByNombre(UUID negocioId);

    Optional<Servicio> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<Servicio> findAllByIdInAndNegocioId(Collection<UUID> ids, UUID negocioId);
}
