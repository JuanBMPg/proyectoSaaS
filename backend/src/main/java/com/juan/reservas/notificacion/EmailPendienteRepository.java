package com.juan.reservas.notificacion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.juan.reservas.reserva.EstadoReserva;
import com.juan.reservas.reserva.Reserva;

public interface EmailPendienteRepository extends JpaRepository<EmailPendiente, UUID> {

    /**
     * Toma los siguientes emails listos para enviar y los bloquea hasta el final de la transacción.
     * <p>
     * {@code FOR UPDATE SKIP LOCKED}: si hay dos instancias de la aplicación, cada una salta las
     * filas que la otra ya tiene bloqueadas, así que un email nunca se envía dos veces a la vez
     * y ninguna espera a la otra. Es SQL nativo porque JPQL no tiene SKIP LOCKED.
     */
    @Query(value = """
            SELECT * FROM email_pendiente
            WHERE estado = 'PENDIENTE' AND proximo_intento <= :ahora
            ORDER BY proximo_intento
            LIMIT :limite
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<EmailPendiente> bloquearListos(Instant ahora, int limite);

    List<EmailPendiente> findAllByReservaIdOrderByCreadoEn(UUID reservaId);

    /**
     * Reservas en ese estado que empiezan en (desde, hasta] y aún no tienen recordatorio en la cola.
     * Trae en la misma consulta todo lo que necesita el email.
     */
    @Query("""
            select r from Reserva r
              join fetch r.negocio join fetch r.servicio join fetch r.recurso join fetch r.cliente
            where r.estado = :estado and r.inicio > :desde and r.inicio <= :hasta
              and not exists (
                select 1 from EmailPendiente e
                where e.reservaId = r.id and e.tipo = com.juan.reservas.notificacion.TipoEmail.RECORDATORIO)
            """)
    List<Reserva> findSinRecordatorio(EstadoReserva estado, Instant desde, Instant hasta);
}
