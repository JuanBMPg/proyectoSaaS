package com.juan.reservas.recurso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.TestDeIntegracion;

/**
 * Verifica las FKs compuestas de la migración V3: la base de datos impide que un recurso
 * preste un servicio de otro negocio, aunque el código tuviera un fallo de filtrado.
 */
@TestDeIntegracion
@Transactional
class ServicioRecursoMismoNegocioIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    private UUID lola;
    private UUID ruiz;

    @BeforeEach
    void crearNegocios() {
        lola = crearNegocio("lola");
        ruiz = crearNegocio("ruiz");
    }

    @Test
    void permiteAsignarUnServicioDelMismoNegocio() {
        asignar(lola, crearServicio(lola), crearRecurso(lola));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM servicio_recurso WHERE negocio_id = ?",
                Integer.class, lola)).isEqualTo(1);
    }

    @Test
    void rechazaUnServicioDeOtroNegocio() {
        UUID servicioDeRuiz = crearServicio(ruiz);
        UUID recursoDeLola = crearRecurso(lola);

        assertThatThrownBy(() -> asignar(lola, servicioDeRuiz, recursoDeLola))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("servicio_recurso_servicio_mismo_negocio_fk");
    }

    @Test
    void rechazaUnRecursoDeOtroNegocio() {
        UUID servicioDeLola = crearServicio(lola);
        UUID recursoDeRuiz = crearRecurso(ruiz);

        assertThatThrownBy(() -> asignar(lola, servicioDeLola, recursoDeRuiz))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("servicio_recurso_recurso_mismo_negocio_fk");
    }

    @Test
    void borrarElServicioBorraSusAsignaciones() {
        UUID servicio = crearServicio(lola);
        asignar(lola, servicio, crearRecurso(lola));

        jdbc.update("DELETE FROM servicio WHERE id = ?", servicio);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM servicio_recurso WHERE servicio_id = ?",
                Integer.class, servicio)).isZero();
    }

    private UUID crearNegocio(String nombre) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO negocio (id, nombre, slug) VALUES (?, ?, ?)", id, nombre, nombre + "-" + id);
        return id;
    }

    private UUID crearServicio(UUID negocioId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO servicio (id, negocio_id, nombre, duracion_min) VALUES (?, ?, 'Servicio', 30)",
                id, negocioId);
        return id;
    }

    private UUID crearRecurso(UUID negocioId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO recurso (id, negocio_id, nombre, tipo) VALUES (?, ?, 'Recurso', 'PERSONA')",
                id, negocioId);
        return id;
    }

    private void asignar(UUID negocioId, UUID servicioId, UUID recursoId) {
        jdbc.update("INSERT INTO servicio_recurso (servicio_id, recurso_id, negocio_id) VALUES (?, ?, ?)",
                servicioId, recursoId, negocioId);
    }
}
