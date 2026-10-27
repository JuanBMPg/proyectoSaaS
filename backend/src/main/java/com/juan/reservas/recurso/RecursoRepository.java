package com.juan.reservas.recurso;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Todas las consultas filtran por negocio: nunca se busca un recurso solo por su id. */
public interface RecursoRepository extends JpaRepository<Recurso, UUID> {

    /**
     * Carga las asignaciones en la misma consulta (JOIN). Sin el EntityGraph, leer los
     * servicios de cada recurso haría una consulta más por recurso (problema N+1).
     */
    @EntityGraph(attributePaths = "asignaciones")
    List<Recurso> findAllByNegocioIdOrderByNombre(UUID negocioId);

    @EntityGraph(attributePaths = "asignaciones")
    Optional<Recurso> findByIdAndNegocioId(UUID id, UUID negocioId);

    boolean existsByIdAndNegocioId(UUID id, UUID negocioId);
}
