package com.juan.reservas.publico;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.juan.reservas.SesionDePrueba;
import com.juan.reservas.TestDeIntegracion;

/**
 * Web pública contra la aplicación real y PostgreSQL. El negocio se monta con la API del panel,
 * como lo haría un propietario, y las reservas se insertan con SQL (crearlas llega en la tarea siguiente).
 */
@TestDeIntegracion
class PublicoIntegrationTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    private SesionDePrueba lola;
    /** Dentro de una semana: cumple la antelación mínima (2 h) y la máxima (30 días). */
    private LocalDate fecha;
    private String corte;
    private String marta;
    private String pedro;

    @BeforeEach
    void montarNegocio() throws Exception {
        lola = SesionDePrueba.registrarPropietario(mvc);
        fecha = LocalDate.now(MADRID).plusDays(7);

        corte = crear("servicios", "{\"nombre\": \"Corte\", \"duracionMin\": 30, \"margenMin\": 0, \"precioCentimos\": 1500}");
        marta = crear("recursos", "{\"nombre\": \"Marta\", \"tipo\": \"PERSONA\"}");
        pedro = crear("recursos", "{\"nombre\": \"Pedro\", \"tipo\": \"PERSONA\"}");
        asignar(marta, corte);
        asignar(pedro, corte);
        horario(marta, "10:00", "12:00");
        horario(pedro, "10:00", "12:00");
    }

    // --- GET /api/publico/{slug} ---

    @Test
    void muestraSoloLoQueSePuedeReservar() throws Exception {
        String tinte = crear("servicios", "{\"nombre\": \"Tinte\", \"duracionMin\": 60, \"margenMin\": 10, \"precioCentimos\": 3500}");
        asignar(marta, corte, tinte);
        mvc.perform(lola.autenticar(delete("/api/panel/servicios/" + tinte))).andExpect(status().isNoContent());
        crear("servicios", "{\"nombre\": \"Peinado\", \"duracionMin\": 45, \"margenMin\": 0, \"precioCentimos\": 2000}");
        String lucia = crear("recursos", "{\"nombre\": \"Lucía\", \"tipo\": \"PERSONA\"}");
        asignar(lucia, corte);
        mvc.perform(lola.autenticar(delete("/api/panel/recursos/" + lucia))).andExpect(status().isNoContent());

        mvc.perform(get("/api/publico/" + lola.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(lola.slug()))
                .andExpect(jsonPath("$.zonaHoraria").value("Europe/Madrid"))
                .andExpect(jsonPath("$.etiquetaRecurso").value("Profesional"))
                // Tinte está inactivo y Peinado no lo presta nadie: no se ofrecen
                .andExpect(jsonPath("$.servicios[*].nombre").value(containsInAnyOrder("Corte")))
                .andExpect(jsonPath("$.servicios[0].precioCentimos").value(1500))
                .andExpect(jsonPath("$.servicios[0].margenMin").doesNotExist())
                .andExpect(jsonPath("$.servicios[0].recursoIds").value(containsInAnyOrder(marta, pedro)))
                // Lucía está desactivada
                .andExpect(jsonPath("$.recursos[*].nombre").value(containsInAnyOrder("Marta", "Pedro")));
    }

    @Test
    void unSlugInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/publico/no-existe-este-negocio")).andExpect(status().isNotFound());
    }

    // --- GET /api/publico/{slug}/disponibilidad ---

    @Test
    void sinRecursoDevuelveLosHuecosDeTodosLosQuePrestanElServicio() throws Exception {
        disponibilidad(corte, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value(fecha.toString()))
                .andExpect(jsonPath("$.zonaHoraria").value("Europe/Madrid"))
                // 10:00-12:00 con servicios de 30 min: 7 inicios por profesional
                .andExpect(jsonPath("$.huecos", hasSize(14)))
                .andExpect(jsonPath("$.huecos[0].inicio").value(instante("10:00").toString()));
    }

    @Test
    void conRecursoDevuelveSoloLosSuyos() throws Exception {
        disponibilidad(corte, marta)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.huecos", hasSize(7)))
                .andExpect(jsonPath("$.huecos[*].recursoId", everyItem(is(marta))));
    }

    @Test
    void unaReservaConfirmadaQuitaSusHuecosYUnaCanceladaNo() throws Exception {
        insertarReserva(marta, "10:00", "11:00", "CONFIRMADA");
        insertarReserva(marta, "10:00", "11:00", "CANCELADA");

        disponibilidad(corte, marta)
                .andExpect(jsonPath("$.huecos", hasSize(3)))
                .andExpect(jsonPath("$.huecos[0].inicio").value(instante("11:00").toString()));
        // A Pedro no le afecta
        disponibilidad(corte, pedro).andExpect(jsonPath("$.huecos", hasSize(7)));
    }

    @Test
    void unServicioDeOtroNegocioDevuelve404() throws Exception {
        SesionDePrueba ruiz = SesionDePrueba.registrarPropietario(mvc);
        String servicioDeRuiz = JsonPath.read(mvc.perform(ruiz.autenticar(post("/api/panel/servicios"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Aceite\", \"duracionMin\": 30, \"margenMin\": 0, \"precioCentimos\": 0}"))
                .andReturn().getResponse().getContentAsString(), "$.id");

        disponibilidad(servicioDeRuiz, null).andExpect(status().isNotFound());
    }

    @Test
    void unRecursoQueNoPrestaElServicioDevuelve404() throws Exception {
        String sala = crear("recursos", "{\"nombre\": \"Sala\", \"tipo\": \"COSA\"}");

        disponibilidad(corte, sala).andExpect(status().isNotFound());
    }

    @Test
    void unServicioDesactivadoDevuelve404() throws Exception {
        mvc.perform(lola.autenticar(delete("/api/panel/servicios/" + corte))).andExpect(status().isNoContent());

        disponibilidad(corte, null).andExpect(status().isNotFound());
    }

    @Test
    void parametrosNoValidosDevuelven400() throws Exception {
        mvc.perform(get("/api/publico/" + lola.slug() + "/disponibilidad")
                        .param("servicioId", corte).param("fecha", "mañana"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/publico/" + lola.slug() + "/disponibilidad").param("fecha", fecha.toString()))
                .andExpect(status().isBadRequest());
    }

    // --- Registro con slug reservado ---

    @Test
    void noSePuedeRegistrarUnNegocioConUnSlugReservado() throws Exception {
        mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreNegocio": "Panel", "slug": "panel", "nombre": "Ana",
                                 "email": "ana-%s@example.com", "password": "secreta123"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.slug").value("esa dirección está reservada por la aplicación"));
    }

    // --- Utilidades ---

    private ResultActions disponibilidad(String servicioId, String recursoId) throws Exception {
        var peticion = get("/api/publico/" + lola.slug() + "/disponibilidad")
                .param("servicioId", servicioId)
                .param("fecha", fecha.toString());
        if (recursoId != null) {
            peticion.param("recursoId", recursoId);
        }
        return mvc.perform(peticion);
    }

    private String crear(String recurso, String json) throws Exception {
        String respuesta = mvc.perform(lola.autenticar(post("/api/panel/" + recurso))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    private void asignar(String recursoId, String... servicioIds) throws Exception {
        mvc.perform(lola.autenticar(put("/api/panel/recursos/" + recursoId + "/servicios"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"servicioIds\": [\"" + String.join("\", \"", servicioIds) + "\"]}"))
                .andExpect(status().isOk());
    }

    /** Una franja el día de la semana de {@code fecha}. */
    private void horario(String recursoId, String inicio, String fin) throws Exception {
        mvc.perform(lola.autenticar(put("/api/panel/recursos/" + recursoId + "/horario"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"franjas\": [{\"diaSemana\": %d, \"horaInicio\": \"%s\", \"horaFin\": \"%s\"}]}"
                                .formatted(fecha.getDayOfWeek().getValue(), inicio, fin)))
                .andExpect(status().isOk());
    }

    private void insertarReserva(String recursoId, String inicio, String fin, String estado) {
        UUID negocioId = jdbc.queryForObject("SELECT id FROM negocio WHERE slug = ?", UUID.class, lola.slug());
        UUID clienteId = UUID.randomUUID();
        jdbc.update("INSERT INTO cliente (id, negocio_id, nombre, email) VALUES (?, ?, 'Juan', ?)",
                clienteId, negocioId, clienteId + "@example.com");
        jdbc.update("""
                INSERT INTO reserva (id, negocio_id, servicio_id, recurso_id, cliente_id,
                                     inicio, fin, fin_bloqueo, estado, token_cancelacion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(), negocioId, UUID.fromString(corte), UUID.fromString(recursoId), clienteId,
                Timestamp.from(instante(inicio)), Timestamp.from(instante(fin)), Timestamp.from(instante(fin)),
                estado, UUID.randomUUID().toString());
    }

    private Instant instante(String hora) {
        return ZonedDateTime.of(fecha, LocalTime.parse(hora), MADRID).toInstant();
    }
}
