package com.juan.reservas.excepcion;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Todas las consultas filtran por negocio, como en el resto de repositorios. */
public interface ExcepcionRepository extends JpaRepository<Excepcion, UUID> {

    /** Las que siguen vigentes en {@code desde} o empiezan después, con su recurso en la misma consulta. */
    @Query("""
            select e from Excepcion e left join fetch e.recurso
            where e.negocio.id = :negocioId and e.fechaFin >= :desde
            order by e.fechaInicio, e.fechaFin
            """)
    List<Excepcion> findVigentes(UUID negocioId, LocalDate desde);

    /** Las de ese día que afectan a alguno de los recursos: suyas o de todo el negocio. */
    @Query("""
            select e from Excepcion e left join fetch e.recurso
            where e.negocio.id = :negocioId
              and e.fechaInicio <= :fecha and e.fechaFin >= :fecha
              and (e.recurso is null or e.recurso.id in :recursoIds)
            """)
    List<Excepcion> findDelDia(UUID negocioId, LocalDate fecha, Collection<UUID> recursoIds);

    Optional<Excepcion> findByIdAndNegocioId(UUID id, UUID negocioId);
}
