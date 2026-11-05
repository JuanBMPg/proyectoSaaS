package com.juan.reservas.empleado;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.juan.reservas.NegocioDePrueba;
import com.juan.reservas.SesionDePrueba;
import com.juan.reservas.TestDeIntegracion;

@TestDeIntegracion
class EmpleadoIntegrationTest {

    private static final String RUTA = "/api/panel/empleados";

    @Autowired
    private MockMvc mvc;

    private NegocioDePrueba lola;
    private String marta;

    @BeforeEach
    void montarNegocio() throws Exception {
        lola = NegocioDePrueba.registrar(mvc);
        marta = lola.crearRecurso("Marta", "PERSONA");
    }

    @Test
    void elPropietarioDaDeAltaUnEmpleadoQuePuedeIniciarSesion() throws Exception {
        String email = unico();
        crear(lola.propietario(), email, marta)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.recurso.nombre").value("Marta"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, SesionDePrueba.PASSWORD)))
                .andExpect(status().isOk());
        mvc.perform(lola.propietario().autenticar(get(RUTA)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].recurso.nombre").value("Marta"));
    }

    @Test
    void unRecursoDeOtroNegocioDevuelve404() throws Exception {
        NegocioDePrueba ruiz = NegocioDePrueba.registrar(mvc);
        String recursoDeRuiz = ruiz.crearRecurso("Mecánico", "PERSONA");

        crear(lola.propietario(), unico(), recursoDeRuiz).andExpect(status().isNotFound());
    }

    @Test
    void unRecursoYaAsignadoOUnEmailRepetidoDevuelven409() throws Exception {
        String email = unico();
        crear(lola.propietario(), email, marta).andExpect(status().isCreated());

        crear(lola.propietario(), unico(), marta)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ese recurso ya tiene un empleado asignado"));
        String pedro = lola.crearRecurso("Pedro", "PERSONA");
        crear(lola.propietario(), email, pedro).andExpect(status().isConflict());
    }

    @Test
    void unEmpleadoNoPuedeGestionarEmpleados() throws Exception {
        SesionDePrueba empleada = lola.crearEmpleado(marta);

        mvc.perform(empleada.autenticar(get(RUTA))).andExpect(status().isForbidden());
    }

    private ResultActions crear(SesionDePrueba sesion, String email, String recursoId) throws Exception {
        return mvc.perform(sesion.autenticar(post(RUTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre": "Marta López", "email": "%s", "password": "%s", "recursoId": "%s"}
                        """.formatted(email, SesionDePrueba.PASSWORD, recursoId)));
    }

    private static String unico() {
        return "empleado-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }
}
