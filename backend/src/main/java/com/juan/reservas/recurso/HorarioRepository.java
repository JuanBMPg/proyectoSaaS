package com.juan.reservas.recurso;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Se accede siempre a través de un recurso ya verificado como del negocio del usuario. */
public interface HorarioRepository extends JpaRepository<Horario, UUID> {

    // Consultas en JPQL explícito: con el getter Horario.getRecursoId(), Spring Data tomaría
    // "RecursoId" como un atributo de Horario en vez de navegar a recurso.id

    @Query("select h from Horario h where h.recurso.id = :recursoId order by h.diaSemana, h.horaInicio")
    List<Horario> findAllByRecursoIdOrderByDiaSemanaAscHoraInicioAsc(UUID recursoId);

    /** Franjas de varios recursos para un día de la semana, en una sola consulta. */
    @Query("select h from Horario h where h.recurso.id in :recursoIds and h.diaSemana = :diaSemana")
    List<Horario> findAllByRecursoIdInAndDiaSemana(Collection<UUID> recursoIds, DayOfWeek diaSemana);

    /**
     * DELETE inmediato en la base de datos (no espera al flush). Así las franjas antiguas
     * desaparecen antes de insertar las nuevas y la restricción horario_sin_solape no
     * compara una franja nueva con una antigua que se va a sustituir.
     */
    @Modifying
    @Query("delete from Horario h where h.recurso.id = :recursoId")
    void borrarDeRecurso(UUID recursoId);
}
