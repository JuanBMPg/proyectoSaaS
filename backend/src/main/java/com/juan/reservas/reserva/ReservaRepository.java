package com.juan.reservas.reserva;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    Optional<Reserva> findByTokenHash(String tokenHash);

    /**
     * Para cambiar su estado: SELECT ... FOR UPDATE. Si llegan dos peticiones a la vez (un doble
     * clic en "Cancelar"), la segunda espera a que acabe la primera y ya ve la reserva cancelada,
     * así que no se envían dos emails ni se pisa el cambio.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.tokenHash = :tokenHash")
    Optional<Reserva> findParaCambiarPorTokenHash(String tokenHash);

    /** Igual que {@link #findParaCambiarPorTokenHash}, para el panel. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id and r.negocio.id = :negocioId")
    Optional<Reserva> findParaCambiar(UUID id, UUID negocioId);

    /** Reservas de la agenda del negocio que se cruzan con [desde, hasta), con sus datos en una consulta. */
    @Query("""
            select r from Reserva r
              join fetch r.servicio join fetch r.recurso join fetch r.cliente
            where r.negocio.id = :negocioId and r.inicio < :hasta and r.fin > :desde
            order by r.inicio
            """)
    List<Reserva> findAgenda(UUID negocioId, Instant desde, Instant hasta);

    /** Igual que {@link #findAgenda}, pero solo de un recurso (la agenda de un empleado). */
    @Query("""
            select r from Reserva r
              join fetch r.servicio join fetch r.recurso join fetch r.cliente
            where r.negocio.id = :negocioId and r.recurso.id = :recursoId
              and r.inicio < :hasta and r.fin > :desde
            order by r.inicio
            """)
    List<Reserva> findAgendaDeRecurso(UUID negocioId, UUID recursoId, Instant desde, Instant hasta);

    /** Cuántas reservas del negocio en esos estados empiezan dentro de [desde, hasta). */
    @Query("""
            select count(r) from Reserva r
            where r.negocio.id = :negocioId and r.estado in :estados
              and r.inicio >= :desde and r.inicio < :hasta
            """)
    long contarEnRango(UUID negocioId, Collection<EstadoReserva> estados, Instant desde, Instant hasta);

    /** Igual que {@link #contarEnRango}, pero solo de un recurso. */
    @Query("""
            select count(r) from Reserva r
            where r.negocio.id = :negocioId and r.recurso.id = :recursoId and r.estado in :estados
              and r.inicio >= :desde and r.inicio < :hasta
            """)
    long contarEnRangoDeRecurso(UUID negocioId, UUID recursoId, Collection<EstadoReserva> estados,
            Instant desde, Instant hasta);

    /**
     * Reservas que ocupan tiempo de estos recursos dentro de [desde, hasta): las que están en
     * uno de los estados dados y cuyo intervalo [inicio, finBloqueo) se cruza con el pedido.
     * Usa el índice idx_reserva_recurso_inicio.
     */
    @Query("""
            select r from Reserva r
            where r.recurso.id in :recursoIds
              and r.estado in :estados
              and r.inicio < :hasta
              and r.finBloqueo > :desde
            """)
    List<Reserva> findQueOcupan(Collection<UUID> recursoIds, Collection<EstadoReserva> estados,
            Instant desde, Instant hasta);
}
