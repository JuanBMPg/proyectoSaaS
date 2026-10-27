package com.juan.reservas.reserva;

import static com.juan.reservas.NegocioDePrueba.instante;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.juan.reservas.NegocioDePrueba;
import com.juan.reservas.TestDeIntegracion;
import com.juan.reservas.comun.TokenSecreto;

/** Crear y consultar reservas desde la web pública, contra la aplicación real y PostgreSQL. */
@TestDeIntegracion
class ReservaPublicaIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    private NegocioDePrueba lola;
    /** Dentro de una semana: cumple la antelación mínima (2 h) y la máxima (30 días). */
    private LocalDate fecha;
    private String corte;
    private String marta;

    @BeforeEach
    void montarNegocio() throws Exception {
        lola = NegocioDePrueba.registrar(mvc);
        fecha = LocalDate.now(NegocioDePrueba.MADRID).plusDays(7);
        corte = lola.crearServicio("Corte", 30, 10, 1500);
        marta = lola.crearRecurso("Marta", "PERSONA");
        lola.asignar(marta, corte);
        lola.horario(marta, fecha, "10:00", "12:00");
    }

    // --- Crear ---

    @Test
    void creaUnaReservaConfirmadaYDevuelveSuEnlace() throws Exception {
        String respuesta = reservar(instante(fecha, "10:15"), "juan@example.com")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/publico/reservas/")))
                .andExpect(jsonPath("$.reserva.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.reserva.inicio").value(instante(fecha, "10:15").toString()))
                .andExpect(jsonPath("$.reserva.fin").value(instante(fecha, "10:45").toString()))
                .andExpect(jsonPath("$.reserva.servicio.nombre").value("Corte"))
                .andExpect(jsonPath("$.reserva.recurso.nombre").value("Marta"))
                .andExpect(jsonPath("$.reserva.recurso.etiqueta").value("Profesional"))
                .andExpect(jsonPath("$.reserva.negocio.slug").value(lola.slug()))
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(respuesta, "$.token");
        Map<String, Object> fila = jdbc.queryForMap(
                "SELECT fin_bloqueo, token_cancelacion FROM reserva WHERE token_cancelacion = ?",
                TokenSecreto.hash(token));
        // El bloqueo incluye los 10 min de margen; en la BD se guarda el hash, no el token
        assertThat(((Timestamp) fila.get("fin_bloqueo")).toInstant()).isEqualTo(instante(fecha, "10:55"));
        assertThat(fila.get("token_cancelacion")).isNotEqualTo(token);
    }

    @Test
    void nacePendienteSiElNegocioConfirmaAMano() throws Exception {
        jdbc.update("UPDATE negocio SET confirmacion_automatica = false WHERE slug = ?", lola.slug());

        reservar(instante(fecha, "10:00"), "juan@example.com")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reserva.estado").value("PENDIENTE"));
    }

    @Test
    void reutilizaElClienteSiElEmailYaExisteYActualizaSusDatos() throws Exception {
        reservar(instante(fecha, "10:00"), "juan@example.com", "Juan").andExpect(status().isCreated());
        reservar(instante(fecha, "11:00"), "JUAN@Example.com", "Juan Pérez").andExpect(status().isCreated());

        assertThat(jdbc.queryForList("SELECT nombre FROM cliente WHERE email = 'juan@example.com' "
                + "AND negocio_id = (SELECT id FROM negocio WHERE slug = ?)", String.class, lola.slug()))
                .containsExactly("Juan Pérez");
    }

    // --- 409: el hueco no está libre ---

    @Test
    void unHuecoOcupadoDevuelve409() throws Exception {
        reservar(instante(fecha, "10:00"), "juan@example.com").andExpect(status().isCreated());

        reservar(instante(fecha, "10:00"), "ana@example.com")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ese hueco acaba de ocuparse"));
        // 10:30 cae dentro del margen de la anterior (bloqueada hasta las 10:40)
        reservar(instante(fecha, "10:30"), "ana@example.com").andExpect(status().isConflict());
    }

    @Test
    void unaHoraFueraDeLaRejillaODelHorarioDevuelve409() throws Exception {
        reservar(instante(fecha, "10:07"), "juan@example.com").andExpect(status().isConflict());
        reservar(instante(fecha, "15:00"), "juan@example.com").andExpect(status().isConflict());
    }

    // --- 422: reglas de antelación ---

    @Test
    void sinLaAntelacionMinimaDevuelve422() throws Exception {
        Instant dentroDeMediaHora = Instant.now().plus(Duration.ofMinutes(30)).truncatedTo(ChronoUnit.MINUTES);

        reservar(dentroDeMediaHora, "juan@example.com")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Las reservas se hacen con al menos 2 horas de antelación"));
    }

    @Test
    void masAllaDeLaAntelacionMaximaDevuelve422() throws Exception {
        reservar(instante(fecha.plusDays(40), "10:00"), "juan@example.com")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Solo se puede reservar con hasta 30 días de antelación"));
    }

    // --- 404 y 400 ---

    @Test
    void unRecursoQueNoPrestaElServicioDevuelve404() throws Exception {
        String sala = lola.crearRecurso("Sala", "COSA");

        mvc.perform(post("/api/publico/" + lola.slug() + "/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(corte, sala, instante(fecha, "10:00"), "juan@example.com", "Juan", null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unServicioDeOtroNegocioDevuelve404() throws Exception {
        NegocioDePrueba ruiz = NegocioDePrueba.registrar(mvc);
        String servicioDeRuiz = ruiz.crearServicio("Aceite", 30, 0, 0);

        mvc.perform(post("/api/publico/" + lola.slug() + "/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(servicioDeRuiz, marta, instante(fecha, "10:00"), "juan@example.com", "Juan", null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void datosDelClienteNoValidosDevuelven400PorCampo() throws Exception {
        mvc.perform(post("/api/publico/" + lola.slug() + "/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(corte, marta, instante(fecha, "10:00"), "no-es-un-email", "", "abc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").exists())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.telefono").exists());
    }

    // --- Consultar por el enlace ---

    @Test
    void seConsultaConSuTokenSinDatosDeContacto() throws Exception {
        String respuesta = reservar(instante(fecha, "11:00"), "juan@example.com", "Juan")
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(respuesta, "$.token");

        mvc.perform(get("/api/publico/reservas/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.clienteNombre").value("Juan"))
                .andExpect(jsonPath("$.servicio.precioCentimos").value(1500))
                .andExpect(jsonPath("$.negocio.zonaHoraria").value("Europe/Madrid"))
                .andExpect(jsonPath("$.clienteEmail").doesNotExist());
    }

    @Test
    void unTokenDesconocidoDevuelve404() throws Exception {
        mvc.perform(get("/api/publico/reservas/" + TokenSecreto.generar()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Esta reserva no existe"));
    }

    // --- Utilidades ---

    private ResultActions reservar(Instant inicio, String email) throws Exception {
        return reservar(inicio, email, "Juan");
    }

    private ResultActions reservar(Instant inicio, String email, String nombre) throws Exception {
        return mvc.perform(post("/api/publico/" + lola.slug() + "/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(corte, marta, inicio, email, nombre, "600 000 000")));
    }

    static String json(String servicioId, String recursoId, Instant inicio, String email, String nombre,
            String telefono) {
        return """
                {"servicioId": "%s", "recursoId": "%s", "inicio": "%s",
                 "nombre": "%s", "email": "%s", "telefono": %s}
                """.formatted(servicioId, recursoId, inicio, nombre, email,
                telefono == null ? "null" : "\"" + telefono + "\"");
    }
}
