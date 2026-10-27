package com.juan.reservas.recurso.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/** Test unitario de las reglas de validación del horario, sin Spring ni base de datos. */
class HorarioRequestTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void crearValidador() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        fabrica.close();
    }

    @Test
    void aceptaUnaJornadaPartida() {
        assertThat(errores(franja(1, "09:00", "14:00"), franja(1, "16:00", "20:00"))).isEmpty();
    }

    @Test
    void aceptaFranjasContiguas() {
        assertThat(errores(franja(1, "09:00", "14:00"), franja(1, "14:00", "18:00"))).isEmpty();
    }

    @Test
    void aceptaLaMismaFranjaEnDiasDistintos() {
        assertThat(errores(franja(1, "09:00", "14:00"), franja(2, "09:00", "14:00"))).isEmpty();
    }

    @Test
    void aceptaUnaSemanaVacia() {
        assertThat(errores()).isEmpty();
    }

    @Test
    void rechazaFranjasSolapadasElMismoDia() {
        assertThat(errores(franja(1, "09:00", "14:00"), franja(1, "13:00", "18:00")))
                .containsExactly("sinSolapes");
    }

    @Test
    void rechazaUnaFranjaContenidaEnOtra() {
        assertThat(errores(franja(3, "09:00", "20:00"), franja(3, "12:00", "13:00")))
                .containsExactly("sinSolapes");
    }

    @Test
    void rechazaUnaFranjaQueTerminaAntesDeEmpezar() {
        assertThat(errores(franja(1, "14:00", "09:00"))).containsExactly("franjas[0].inicioAntesDeFin");
    }

    @Test
    void rechazaUnaFranjaDeDuracionCero() {
        assertThat(errores(franja(1, "09:00", "09:00"))).containsExactly("franjas[0].inicioAntesDeFin");
    }

    @Test
    void rechazaUnDiaFueraDeRango() {
        assertThat(errores(franja(8, "09:00", "14:00"))).containsExactly("franjas[0].diaSemana");
    }

    private static FranjaHorariaRequest franja(int dia, String inicio, String fin) {
        return new FranjaHorariaRequest(dia, LocalTime.parse(inicio), LocalTime.parse(fin));
    }

    /** Devuelve las rutas de las propiedades con error, p. ej. "franjas[0].diaSemana". */
    private static List<String> errores(FranjaHorariaRequest... franjas) {
        Set<ConstraintViolation<HorarioRequest>> violaciones = validador.validate(new HorarioRequest(List.of(franjas)));
        return violaciones.stream().map(v -> v.getPropertyPath().toString()).sorted().toList();
    }
}
