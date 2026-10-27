package com.juan.reservas.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.TestDeIntegracion;

/**
 * Verifica las FKs compuestas de la migración V2: una reserva no puede usar el servicio,
 * el recurso ni el cliente de otro negocio, aunque el código tuviera un fallo de filtrado.
 */
@TestDeIntegracion
@Transactional
class ReservaMismoNegocioIntegrationTest {

    /** SQLState de PostgreSQL para foreign_key_violation. */
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    private static final Instant INICIO = Instant.parse("2026-06-02T08:00:00Z");

    @Autowired
    private JdbcTemplate jdbc;

    private Negocio lola;
    private Negocio ruiz;

    @BeforeEach
    void crearDosNegocios() {
        lola = crearNegocio("lola");
        ruiz = crearNegocio("ruiz");
    }

    @Test
    void permiteUnaReservaConDatosDelMismoNegocio() {
        reservar(lola.id, lola.servicioId, lola.recursoId, lola.clienteId);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva WHERE negocio_id = ?",
                Integer.class, lola.id)).isEqualTo(1);
    }

    @Test
    void rechazaUnRecursoDeOtroNegocio() {
        assertThatThrownBy(() -> reservar(lola.id, lola.servicioId, ruiz.recursoId, lola.clienteId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertThat(sqlState(ex)).isEqualTo(FOREIGN_KEY_VIOLATION))
                .hasMessageContaining("reserva_recurso_mismo_negocio_fk");
    }

    @Test
    void rechazaUnServicioDeOtroNegocio() {
        assertThatThrownBy(() -> reservar(lola.id, ruiz.servicioId, lola.recursoId, lola.clienteId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("reserva_servicio_mismo_negocio_fk");
    }

    @Test
    void rechazaUnClienteDeOtroNegocio() {
        assertThatThrownBy(() -> reservar(lola.id, lola.servicioId, lola.recursoId, ruiz.clienteId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("reserva_cliente_mismo_negocio_fk");
    }

    @Test
    void borrarUnNegocioSigueBorrandoEnCascadaSusReservas() {
        reservar(lola.id, lola.servicioId, lola.recursoId, lola.clienteId);

        jdbc.update("DELETE FROM negocio WHERE id = ?", lola.id);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva WHERE negocio_id = ?",
                Integer.class, lola.id)).isZero();
    }

    private record Negocio(UUID id, UUID servicioId, UUID recursoId, UUID clienteId) {
    }

    private Negocio crearNegocio(String nombre) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO negocio (id, nombre, slug) VALUES (?, ?, ?)", id, nombre, nombre + "-" + id);

        UUID servicioId = UUID.randomUUID();
        jdbc.update("INSERT INTO servicio (id, negocio_id, nombre, duracion_min) VALUES (?, ?, 'Servicio', 30)",
                servicioId, id);

        UUID recursoId = UUID.randomUUID();
        jdbc.update("INSERT INTO recurso (id, negocio_id, nombre, tipo) VALUES (?, ?, 'Recurso', 'PERSONA')",
                recursoId, id);

        UUID clienteId = UUID.randomUUID();
        jdbc.update("INSERT INTO cliente (id, negocio_id, nombre, email) VALUES (?, ?, 'Cliente', 'cliente@example.com')",
                clienteId, id);

        return new Negocio(id, servicioId, recursoId, clienteId);
    }

    private void reservar(UUID negocioId, UUID servicioId, UUID recursoId, UUID clienteId) {
        Timestamp inicio = Timestamp.from(INICIO);
        Timestamp fin = Timestamp.from(INICIO.plusSeconds(30 * 60));
        jdbc.update("""
                INSERT INTO reserva (id, negocio_id, servicio_id, recurso_id, cliente_id,
                                     inicio, fin, fin_bloqueo, estado, token_cancelacion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'CONFIRMADA', ?)
                """,
                UUID.randomUUID(), negocioId, servicioId, recursoId, clienteId,
                inicio, fin, fin, UUID.randomUUID().toString());
    }

    private static String sqlState(Throwable ex) {
        for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
            if (causa instanceof SQLException sql) {
                return sql.getSQLState();
            }
        }
        return null;
    }
}
