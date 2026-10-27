package com.juan.reservas.recurso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Time;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.TestDeIntegracion;

/**
 * Verifica la restricción de exclusión horario_sin_solape de la migración V4 directamente
 * en PostgreSQL, sin pasar por la validación de la aplicación.
 */
@TestDeIntegracion
@Transactional
class HorarioSinSolapeIntegrationTest {

    private static final int LUNES = 1;
    private static final int MARTES = 2;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID marta;
    private UUID lucia;

    @BeforeEach
    void crearRecursos() {
        UUID negocio = UUID.randomUUID();
        jdbc.update("INSERT INTO negocio (id, nombre, slug) VALUES (?, 'Lola', ?)", negocio, "lola-" + negocio);
        marta = crearRecurso(negocio, "Marta");
        lucia = crearRecurso(negocio, "Lucía");
    }

    @Test
    void rechazaDosFranjasSolapadasDelMismoRecursoYDia() {
        franja(marta, LUNES, "09:00", "14:00");

        assertThatThrownBy(() -> franja(marta, LUNES, "13:00", "18:00"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("horario_sin_solape");
    }

    @Test
    void permiteFranjasContiguas() {
        franja(marta, LUNES, "09:00", "14:00");
        franja(marta, LUNES, "14:00", "18:00");

        assertThat(franjasDe(marta)).isEqualTo(2);
    }

    @Test
    void permiteLaMismaFranjaOtroDiaUOtroRecurso() {
        franja(marta, LUNES, "09:00", "14:00");
        franja(marta, MARTES, "09:00", "14:00");
        franja(lucia, LUNES, "09:00", "14:00");

        assertThat(franjasDe(marta) + franjasDe(lucia)).isEqualTo(3);
    }

    private UUID crearRecurso(UUID negocio, String nombre) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO recurso (id, negocio_id, nombre, tipo) VALUES (?, ?, ?, 'PERSONA')",
                id, negocio, nombre);
        return id;
    }

    private void franja(UUID recurso, int dia, String inicio, String fin) {
        jdbc.update("INSERT INTO horario (id, recurso_id, dia_semana, hora_inicio, hora_fin) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), recurso, dia,
                Time.valueOf(LocalTime.parse(inicio)), Time.valueOf(LocalTime.parse(fin)));
    }

    private int franjasDe(UUID recurso) {
        return jdbc.queryForObject("SELECT count(*) FROM horario WHERE recurso_id = ?", Integer.class, recurso);
    }
}
