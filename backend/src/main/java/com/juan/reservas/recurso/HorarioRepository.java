package com.juan.reservas.recurso;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Se accede siempre a través de un recurso ya verificado como del negocio del usuario. */
public interface HorarioRepository extends JpaRepository<Horario, UUID> {

    List<Horario> findAllByRecursoIdOrderByDiaSemanaAscHoraInicioAsc(UUID recursoId);

    /**
     * DELETE inmediato en la base de datos (no espera al flush). Así las franjas antiguas
     * desaparecen antes de insertar las nuevas y la restricción horario_sin_solape no
     * compara una franja nueva con una antigua que se va a sustituir.
     */
    @Modifying
    @Query("delete from Horario h where h.recurso.id = :recursoId")
    void borrarDeRecurso(UUID recursoId);
}
