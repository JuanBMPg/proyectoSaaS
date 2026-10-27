package com.juan.reservas.recurso;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.juan.reservas.SesionDePrueba;
import com.juan.reservas.TestDeIntegracion;

/**
 * CRUD de /api/panel/recursos y asignación de servicios contra la aplicación real,
 * incluido el aislamiento entre negocios.
 */
@TestDeIntegracion
class RecursoIntegrationTest {

    private static final String RUTA = "/api/panel/recursos";

    @Autowired
    private MockMvc mvc;

    private SesionDePrueba lola;

    @BeforeEach
    void registrarNegocio() throws Exception {
        lola = SesionDePrueba.registrarPropietario(mvc);
    }

    @Test
    void creaEditaYDesactivaUnRecurso() throws Exception {
        String id = crearRecurso(lola, "Marta", "PERSONA");

        mvc.perform(lola.autenticar(put(RUTA + "/" + id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recursoJson("Marta López", "PERSONA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Marta López"))
                .andExpect(jsonPath("$.servicioIds").value(empty()));

        mvc.perform(lola.autenticar(delete(RUTA + "/" + id))).andExpect(status().isNoContent());

        mvc.perform(lola.autenticar(get(RUTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')].activo".formatted(id)).value(false));
    }

    @Test
    void rechazaUnTipoDeRecursoDesconocido() throws Exception {
        mvc.perform(lola.autenticar(post(RUTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recursoJson("Pista 2", "PISTA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void asignarServiciosSustituyeElConjuntoCompleto() throws Exception {
        String marta = crearRecurso(lola, "Marta", "PERSONA");
        String corte = crearServicio(lola, "Corte");
        String tinte = crearServicio(lola, "Tinte");
        String mechas = crearServicio(lola, "Mechas");

        asignar(lola, marta, corte, tinte)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicioIds").value(containsInAnyOrder(corte, tinte)));

        // Se mantiene uno (tinte), se quita otro (corte) y se añade uno nuevo (mechas)
        asignar(lola, marta, tinte, mechas)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicioIds").value(containsInAnyOrder(tinte, mechas)));

        mvc.perform(lola.autenticar(get(RUTA)))
                .andExpect(jsonPath("$[?(@.id == '%s')].servicioIds[*]".formatted(marta))
                        .value(containsInAnyOrder(tinte, mechas)));

        asignar(lola, marta)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicioIds").value(empty()));
    }

    @Test
    void noSePuedeAsignarUnServicioDeOtroNegocio() throws Exception {
        String marta = crearRecurso(lola, "Marta", "PERSONA");
        String corte = crearServicio(lola, "Corte");
        asignar(lola, marta, corte).andExpect(status().isOk());

        SesionDePrueba ruiz = SesionDePrueba.registrarPropietario(mvc);
        String servicioDeRuiz = crearServicio(ruiz, "Cambio de aceite");

        asignar(lola, marta, corte, servicioDeRuiz).andExpect(status().isNotFound());

        // No ha cambiado nada
        mvc.perform(lola.autenticar(get(RUTA)))
                .andExpect(jsonPath("$[?(@.id == '%s')].servicioIds[*]".formatted(marta))
                        .value(containsInAnyOrder(corte)));
    }

    @Test
    void otroNegocioNoVeNiPuedeTocarElRecurso() throws Exception {
        String marta = crearRecurso(lola, "Marta", "PERSONA");
        SesionDePrueba ruiz = SesionDePrueba.registrarPropietario(mvc);
        String servicioDeRuiz = crearServicio(ruiz, "Cambio de aceite");

        mvc.perform(ruiz.autenticar(get(RUTA)))
                .andExpect(jsonPath("$[*].id").value(not(hasItem(marta))));
        mvc.perform(ruiz.autenticar(put(RUTA + "/" + marta))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recursoJson("Robado", "PERSONA")))
                .andExpect(status().isNotFound());
        mvc.perform(ruiz.autenticar(delete(RUTA + "/" + marta)))
                .andExpect(status().isNotFound());
        asignar(ruiz, marta, servicioDeRuiz).andExpect(status().isNotFound());

        mvc.perform(lola.autenticar(get(RUTA)))
                .andExpect(jsonPath("$[?(@.id == '%s')].nombre".formatted(marta)).value("Marta"))
                .andExpect(jsonPath("$[?(@.id == '%s')].activo".formatted(marta)).value(true));
    }

    private ResultActions asignar(SesionDePrueba sesion, String recursoId, String... servicioIds)
            throws Exception {
        String ids = String.join("\", \"", servicioIds);
        String json = servicioIds.length == 0
                ? "{\"servicioIds\": []}"
                : "{\"servicioIds\": [\"" + ids + "\"]}";
        return mvc.perform(sesion.autenticar(put(RUTA + "/" + recursoId + "/servicios"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private String crearRecurso(SesionDePrueba sesion, String nombre, String tipo) throws Exception {
        String respuesta = mvc.perform(sesion.autenticar(post(RUTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recursoJson(nombre, tipo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    private String crearServicio(SesionDePrueba sesion, String nombre) throws Exception {
        String respuesta = mvc.perform(sesion.autenticar(post("/api/panel/servicios"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "%s", "duracionMin": 30, "margenMin": 0, "precioCentimos": 1500}
                                """.formatted(nombre)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    private static String recursoJson(String nombre, String tipo) {
        return """
                {"nombre": "%s", "tipo": "%s"}
                """.formatted(nombre, tipo);
    }
}
