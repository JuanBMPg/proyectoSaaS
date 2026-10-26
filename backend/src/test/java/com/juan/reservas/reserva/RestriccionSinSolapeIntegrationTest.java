package com.juan.reservas.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.juan.reservas.TestcontainersConfiguration;

/**
 * Verifica contra un PostgreSQL real que la restricción de exclusión reserva_sin_solape
 * impide las dobles reservas en la propia base de datos.
 * <p>
 * Arranca el contexto completo de Spring, así que también comprueba que Flyway aplica
 * la migración V1 y que la configuración de la aplicación es válida.
 * Usa SQL directo porque todavía no existen las entidades JPA (llegan en la fase 1).
 * Cada test se revierte al terminar gracias a {@code @Transactional}.
 * Si no hay Docker disponible el test se marca como omitido (en el CI siempre hay Docker).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class RestriccionSinSolapeIntegrationTest {

    /** Martes 10:00 en Madrid (horario de verano = UTC+2). Fecha fija: nada de Instant.now(). */
    private static final Instant DIEZ = Instant.parse("2026-06-02T08:00:00Z");
    private static final Duration DURACION = Duration.ofMinutes(30);
    private static final Duration MARGEN = Duration.ofMinutes(10);

    @Autowired
    private JdbcTemplate jdbc;

    private UUID negocioId;
    private UUID servicioId;
    private UUID clienteId;
    private UUID marta;
    private UUID lucia;

    @BeforeEach
    void crearDatosBase() {
        negocioId = UUID.randomUUID();
        jdbc.update("INSERT INTO negocio (id, nombre, slug) VALUES (?, 'Peluquería Lola', ?)",
                negocioId, "lola-" + negocioId);

        servicioId = UUID.randomUUID();
        jdbc.update("INSERT INTO servicio (id, negocio_id, nombre, duracion_min, margen_min) "
                + "VALUES (?, ?, 'Corte de pelo', ?, ?)",
                servicioId, negocioId, DURACION.toMinutes(), MARGEN.toMinutes());

        clienteId = UUID.randomUUID();
        jdbc.update("INSERT INTO cliente (id, negocio_id, nombre, email) VALUES (?, ?, 'Juan', 'juan@example.com')",
                clienteId, negocioId);

        marta = crearRecurso("Marta");
        lucia = crearRecurso("Lucía");
    }

    @Test
    void flywayAplicaLaMigracionV1() {
        Integer aplicadas = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success",
                Integer.class);

        assertThat(aplicadas).isEqualTo(1);
    }

    @Test
    void rechazaDosReservasActivasSolapadasDelMismoRecurso() {
        reservar(marta, DIEZ, "CONFIRMADA");

        // Empieza a las 10:15, dentro de la primera cita (10:00-10:30)
        assertThatThrownBy(() -> reservar(marta, DIEZ.plus(Duration.ofMinutes(15)), "PENDIENTE"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(ex -> assertThat(sqlState(ex)).isEqualTo("23P01"))
                .hasMessageContaining("reserva_sin_solape");
    }

    @Test
    void elMargenPosteriorTambienBloqueaElHueco() {
        reservar(marta, DIEZ, "CONFIRMADA");

        // 10:35 cae en el margen (fin 10:30, fin_bloqueo 10:40)
        assertThatThrownBy(() -> reservar(marta, DIEZ.plus(Duration.ofMinutes(35)), "CONFIRMADA"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void permiteUnaReservaQueEmpiezaJustoAlTerminarElBloqueo() {
        reservar(marta, DIEZ, "CONFIRMADA");

        // El rango es [inicio, fin_bloqueo): las 10:40 ya están libres
        reservar(marta, DIEZ.plus(DURACION).plus(MARGEN), "CONFIRMADA");

        assertThat(reservasDe(marta)).isEqualTo(2);
    }

    @Test
    void permiteElMismoHorarioConOtroRecurso() {
        reservar(marta, DIEZ, "CONFIRMADA");
        reservar(lucia, DIEZ, "CONFIRMADA");

        assertThat(reservasDe(marta) + reservasDe(lucia)).isEqualTo(2);
    }

    @Test
    void unaReservaCanceladaLiberaElHueco() {
        reservar(marta, DIEZ, "CANCELADA");
        reservar(marta, DIEZ, "CONFIRMADA");

        assertThat(reservasDe(marta)).isEqualTo(2);
    }

    private UUID crearRecurso(String nombre) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO recurso (id, negocio_id, nombre, tipo) VALUES (?, ?, ?, 'PERSONA')",
                id, negocioId, nombre);
        return id;
    }

    private void reservar(UUID recursoId, Instant inicio, String estado) {
        Instant fin = inicio.plus(DURACION);
        jdbc.update("""
                INSERT INTO reserva (id, negocio_id, servicio_id, recurso_id, cliente_id,
                                     inicio, fin, fin_bloqueo, estado, token_cancelacion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(), negocioId, servicioId, recursoId, clienteId,
                Timestamp.from(inicio), Timestamp.from(fin), Timestamp.from(fin.plus(MARGEN)),
                estado, UUID.randomUUID().toString());
    }

    private int reservasDe(UUID recursoId) {
        return jdbc.queryForObject("SELECT count(*) FROM reserva WHERE recurso_id = ?", Integer.class, recursoId);
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
