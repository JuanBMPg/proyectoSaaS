package com.juan.reservas.notificacion;

import static com.juan.reservas.NegocioDePrueba.instante;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;
import com.juan.reservas.NegocioDePrueba;
import com.juan.reservas.TestDeIntegracion;
import com.juan.reservas.comun.TokenSecreto;
import com.juan.reservas.reserva.ReservaCreada;

/**
 * El email de confirmación de punta a punta: la reserva se crea por HTTP, el email pasa por la
 * cola (outbox) y se comprueba en Mailpit (contenedor real) qué ha llegado. La cola se vacía
 * cada medio segundo en los tests, así que se espera con Awaitility en lugar de dormir.
 */
@TestDeIntegracion
class EmailReservaIntegrationTest {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Autowired
    private MockMvc mvc;
    @Value("${mailpit.api}")
    private String mailpit;

    private NegocioDePrueba lola;
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

    @Test
    void trasCrearLaReservaLlegaUnEmailConElEnlaceASuPagina() throws Exception {
        String email = unico("juan");
        String respuesta = reservar(instante(fecha, "10:00"), email)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(respuesta, "$.token");

        await().atMost(Duration.ofSeconds(10)).until(() -> emailsPara(email).size() == 1);

        String mensaje = obtener("/api/v1/message/" + emailsPara(email).getFirst());
        assertThat((String) JsonPath.read(mensaje, "$.Subject")).startsWith("Tu cita en Negocio ").endsWith(", 10:00");
        assertThat((String) JsonPath.read(mensaje, "$.Text"))
                .contains("Tu cita está confirmada")
                .contains("http://localhost:4200/reserva/" + token);
        assertThat((String) JsonPath.read(mensaje, "$.HTML")).contains("href=\"http://localhost:4200/reserva/" + token + "\"");
        assertThat((String) JsonPath.read(mensaje, "$.From.Name")).startsWith("Negocio ");
    }

    /**
     * Prueba directa del outbox: el evento de una reserva real se publica dentro de una
     * transacción que se deshace. El email que el listener deja en la cola se deshace con ella,
     * así que no llega un segundo email.
     */
    @Test
    void siLaTransaccionSeDeshaceNoSeEnviaEmail(@Autowired ApplicationEventPublisher eventos,
            @Autowired PlatformTransactionManager transacciones, @Autowired JdbcTemplate jdbc) throws Exception {
        String email = unico("rollback");
        String respuesta = reservar(instante(fecha, "11:00"), email)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        await().atMost(Duration.ofSeconds(10)).until(() -> emailsPara(email).size() == 1);
        String token = JsonPath.read(respuesta, "$.token");
        UUID reservaId = jdbc.queryForObject("SELECT id FROM reserva WHERE token_cancelacion = ?",
                UUID.class, TokenSecreto.hash(token));

        new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            eventos.publishEvent(new ReservaCreada(reservaId, "otro-token"));
            estado.setRollbackOnly();
        });

        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(3))
                .until(() -> emailsPara(email).size() == 1);
    }

    @Test
    void trasEnviarseLaColaYaNoGuardaElEnlaceSecreto(@Autowired JdbcTemplate jdbc) throws Exception {
        String respuesta = reservar(instante(fecha, "10:00"), unico("limpio"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID reservaId = jdbc.queryForObject("SELECT id FROM reserva WHERE token_cancelacion = ?",
                UUID.class, TokenSecreto.hash(JsonPath.read(respuesta, "$.token")));

        await().atMost(Duration.ofSeconds(10)).until(() -> "ENVIADO".equals(jdbc.queryForObject(
                "SELECT estado FROM email_pendiente WHERE reserva_id = ?", String.class, reservaId)));

        Integer conCuerpo = jdbc.queryForObject("SELECT count(*) FROM email_pendiente "
                + "WHERE reserva_id = ? AND (texto IS NOT NULL OR html IS NOT NULL)", Integer.class, reservaId);
        assertThat(conCuerpo).isZero();
    }

    /** La reserva rechazada falla en el INSERT, antes de publicar el evento: no hay email. */
    @Test
    void unaReservaRechazadaPorQueElHuecoEstaOcupadoNoEnviaEmail() throws Exception {
        String primero = unico("primero");
        String segundo = unico("segundo");
        reservar(instante(fecha, "10:00"), primero).andExpect(status().isCreated());
        reservar(instante(fecha, "10:00"), segundo).andExpect(status().isConflict());

        // El de la reserva válida llega; el de la rechazada no debe llegar nunca
        await().atMost(Duration.ofSeconds(10)).until(() -> emailsPara(primero).size() == 1);
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(3))
                .until(() -> emailsPara(segundo).isEmpty());
    }

    // --- Utilidades ---

    private ResultActions reservar(Instant inicio, String email) throws Exception {
        return mvc.perform(post("/api/publico/" + lola.slug() + "/reservas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"servicioId": "%s", "recursoId": "%s", "inicio": "%s",
                         "nombre": "Juan", "email": "%s", "telefono": null}
                        """.formatted(corte, marta, inicio, email)));
    }

    /** Ids de los mensajes de Mailpit enviados a esa dirección. */
    private List<String> emailsPara(String email) throws Exception {
        String consulta = URLEncoder.encode("to:\"" + email + "\"", StandardCharsets.UTF_8);
        return JsonPath.read(obtener("/api/v1/search?query=" + consulta), "$.messages[*].ID");
    }

    private String obtener(String ruta) throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(mailpit + ruta)).GET().build();
        return HTTP.send(peticion, HttpResponse.BodyHandlers.ofString()).body();
    }

    /** Todos los tests comparten el mismo Mailpit: cada uno usa direcciones propias. */
    private static String unico(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }
}
