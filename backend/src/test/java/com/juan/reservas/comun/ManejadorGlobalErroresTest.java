package com.juan.reservas.comun;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/** Comprueba que cada tipo de error se traduce al código HTTP y al formato Problem Details esperados. */
class ManejadorGlobalErroresTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new ManejadorGlobalErrores())
                .build();
    }

    @Test
    void noEncontradoDevuelve404() throws Exception {
        mvc.perform(get("/prueba/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Servicio no encontrado"));
    }

    @Test
    void reglaNegocioDevuelve422() throws Exception {
        mvc.perform(get("/prueba/regla"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Fuera de plazo"));
    }

    @Test
    void solapeEnBaseDeDatosDevuelve409ConMensajeDeHuecoOcupado() throws Exception {
        mvc.perform(get("/prueba/solape"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ese hueco acaba de ocuparse"));
    }

    @Test
    void validacionDevuelve400ConErroresPorCampo() throws Exception {
        mvc.perform(post("/prueba/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
    }

    @Test
    void errorInesperadoDevuelve500SinDetallesInternos() throws Exception {
        mvc.perform(get("/prueba/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Ha ocurrido un error inesperado"));
    }

    record PeticionDePrueba(@NotBlank String nombre) {
    }

    @RestController
    static class ControladorDePrueba {

        @GetMapping("/prueba/no-encontrado")
        void noEncontrado() {
            throw new NoEncontradoException("Servicio no encontrado");
        }

        @GetMapping("/prueba/regla")
        void regla() {
            throw new ReglaNegocioException("Fuera de plazo");
        }

        @GetMapping("/prueba/solape")
        void solape() {
            SQLException sql = new SQLException("conflicting key value violates exclusion constraint",
                    ManejadorGlobalErrores.SQLSTATE_EXCLUSION_VIOLATION);
            throw new DataIntegrityViolationException("reserva_sin_solape", sql);
        }

        @PostMapping("/prueba/validacion")
        void validacion(@Valid @RequestBody PeticionDePrueba peticion) {
        }

        @GetMapping("/prueba/inesperado")
        void inesperado() {
            throw new IllegalStateException("detalle interno que no debe salir");
        }
    }
}
