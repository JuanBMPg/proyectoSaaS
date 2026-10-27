package com.juan.reservas.recurso;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.juan.reservas.SesionDePrueba;
import com.juan.reservas.TestDeIntegracion;
import com.juan.reservas.auth.UsuarioRepository;
import com.juan.reservas.negocio.NegocioRepository;

/** GET y PUT de /api/panel/recursos/{id}/horario contra la aplicación real y PostgreSQL. */
@TestDeIntegracion
class HorarioIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private SesionDePrueba lola;
    private String marta;

    @BeforeEach
    void crearRecurso() throws Exception {
        lola = SesionDePrueba.registrarPropietario(mvc);
        String respuesta = mvc.perform(lola.autenticar(post("/api/panel/recursos"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Marta\", \"tipo\": \"PERSONA\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        marta = JsonPath.read(respuesta, "$.id");
    }

    @Test
    void unRecursoNuevoNoTieneHorario() throws Exception {
        mvc.perform(lola.autenticar(get(ruta(marta))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.franjas", hasSize(0)));
    }

    @Test
    void guardaLaSemanaYLaDevuelveOrdenadaConHorasHHmm() throws Exception {
        guardar(lola, marta, """
                [{"diaSemana": 2, "horaInicio": "10:00", "horaFin": "14:00"},
                 {"diaSemana": 1, "horaInicio": "16:00", "horaFin": "20:00"},
                 {"diaSemana": 1, "horaInicio": "09:00", "horaFin": "14:00"}]
                """).andExpect(status().isOk());

        mvc.perform(lola.autenticar(get(ruta(marta))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.franjas", hasSize(3)))
                .andExpect(jsonPath("$.franjas[0].diaSemana").value(1))
                .andExpect(jsonPath("$.franjas[0].horaInicio").value("09:00"))
                .andExpect(jsonPath("$.franjas[0].horaFin").value("14:00"))
                .andExpect(jsonPath("$.franjas[1].horaInicio").value("16:00"))
                .andExpect(jsonPath("$.franjas[2].diaSemana").value(2));
    }

    /**
     * La franja nueva (10-15) se solapa con la antigua (09-14). Solo funciona porque las
     * antiguas se borran antes de insertar; si no, horario_sin_solape la rechazaría.
     */
    @Test
    void sustituirLaSemanaReemplazaLasFranjasAnteriores() throws Exception {
        guardar(lola, marta, "[{\"diaSemana\": 1, \"horaInicio\": \"09:00\", \"horaFin\": \"14:00\"}]")
                .andExpect(status().isOk());

        guardar(lola, marta, "[{\"diaSemana\": 1, \"horaInicio\": \"10:00\", \"horaFin\": \"15:00\"}]")
                .andExpect(status().isOk());

        mvc.perform(lola.autenticar(get(ruta(marta))))
                .andExpect(jsonPath("$.franjas", hasSize(1)))
                .andExpect(jsonPath("$.franjas[0].horaInicio").value("10:00"));
    }

    @Test
    void unaListaVaciaBorraElHorario() throws Exception {
        guardar(lola, marta, "[{\"diaSemana\": 1, \"horaInicio\": \"09:00\", \"horaFin\": \"14:00\"}]");

        guardar(lola, marta, "[]").andExpect(status().isOk());

        mvc.perform(lola.autenticar(get(ruta(marta)))).andExpect(jsonPath("$.franjas", hasSize(0)));
    }

    @Test
    void rechazaFranjasSolapadasCon400() throws Exception {
        guardar(lola, marta, """
                [{"diaSemana": 1, "horaInicio": "09:00", "horaFin": "14:00"},
                 {"diaSemana": 1, "horaInicio": "13:00", "horaFin": "18:00"}]
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.sinSolapes").value("hay franjas que se solapan en el mismo día"));
    }

    @Test
    void rechazaUnaFranjaQueTerminaAntesDeEmpezar() throws Exception {
        guardar(lola, marta, "[{\"diaSemana\": 1, \"horaInicio\": \"14:00\", \"horaFin\": \"09:00\"}]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores['franjas[0].inicioAntesDeFin']").exists());
    }

    @Test
    void otroNegocioRecibe404AlVerOGuardarElHorario() throws Exception {
        SesionDePrueba ruiz = SesionDePrueba.registrarPropietario(mvc);

        mvc.perform(ruiz.autenticar(get(ruta(marta)))).andExpect(status().isNotFound());
        guardar(ruiz, marta, "[{\"diaSemana\": 1, \"horaInicio\": \"09:00\", \"horaFin\": \"14:00\"}]")
                .andExpect(status().isNotFound());

        mvc.perform(lola.autenticar(get(ruta(marta)))).andExpect(jsonPath("$.franjas", hasSize(0)));
    }

    @Test
    void unEmpleadoNoPuedeVerNiCambiarHorarios(
            @Autowired NegocioRepository negocios, @Autowired UsuarioRepository usuarios,
            @Autowired PasswordEncoder passwordEncoder, @Autowired Clock clock) throws Exception {
        SesionDePrueba empleado = lola.crearEmpleado(mvc, negocios, usuarios, passwordEncoder, clock);

        mvc.perform(empleado.autenticar(get(ruta(marta)))).andExpect(status().isForbidden());
        guardar(empleado, marta, "[]").andExpect(status().isForbidden());
    }

    private ResultActions guardar(SesionDePrueba sesion, String recursoId, String franjas) throws Exception {
        return mvc.perform(sesion.autenticar(put(ruta(recursoId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"franjas\": " + franjas + "}"));
    }

    private static String ruta(String recursoId) {
        return "/api/panel/recursos/" + recursoId + "/horario";
    }
}
