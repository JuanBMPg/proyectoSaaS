package com.juan.reservas.reserva;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    Optional<Reserva> findByTokenHash(String tokenHash);

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
