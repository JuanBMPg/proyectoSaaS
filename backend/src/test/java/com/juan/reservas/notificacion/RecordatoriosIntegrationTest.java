package com.juan.reservas.notificacion;

import static com.juan.reservas.NegocioDePrueba.instante;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.juan.reservas.NegocioDePrueba;
import com.juan.reservas.TestDeIntegracion;
import com.juan.reservas.comun.TokenSecreto;

/**
 * Recordatorios 24 h antes. La tarea programada se llama a mano con un "ahora" elegido, así
 * el test no depende del reloj. Las comprobaciones son por reserva: la base de datos la
 * comparten todos los tests y puede haber otras citas en la misma ventana.
 */
@TestDeIntegracion
class RecordatoriosIntegrationTest {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ProgramadorRecordatorios programador;
    @Autowired
    private JdbcTemplate jdbc;
    @Value("${mailpit.api}")
    private String mailpit;

    private NegocioDePrueba lola;
    private Instant inicio;
    private String corte;
    private String marta;

    @BeforeEach
    void montarNegocio() throws Exception {
        lola = NegocioDePrueba.registrar(mvc);
        // Un día que no usan otros tests, para que la ventana de 24 h tenga pocas citas ajenas
        LocalDate fecha = LocalDate.now(NegocioDePrueba.MADRID).plusDays(9);
        corte = lola.crearServicio("Corte", 30, 0, 1500);
        marta = lola.crearRecurso("Marta", "PERSONA");
        lola.asignar(marta, corte);
        lola.horario(marta, fecha, "10:00", "12:00");
        inicio = instante(fecha, "10:00");
    }

    @Test
    void unaCitaConfirmadaRecibeUnSoloRecordatorioUnDiaAntes() throws Exception {
        String email = unico("recordar");
        UUID reserva = reservar(email);

        programador.programar(inicio.minus(Duration.ofHours(23)));
        programador.programar(inicio.minus(Duration.ofHours(22)));

        assertThat(recordatoriosDe(reserva)).isEqualTo(1);
        // Llegan la confirmación y el recordatorio
        await().atMost(Duration.ofSeconds(10)).until(() -> asuntosPara(email).size() == 2);
        assertThat(asuntosPara(email)).anySatisfy(asunto -> assertThat(asunto).startsWith("Recordatorio: tu cita"));
    }

    @Test
    void noSeProgramaAntesDeLaVentanaDe24Horas() throws Exception {
        UUID reserva = reservar(unico("pronto"));

        programador.programar(inicio.minus(Duration.ofHours(25)));

        assertThat(recordatoriosDe(reserva)).isZero();
    }

    @Test
    void siSeReservoConMenosDe24HorasNoHayRecordatorio() throws Exception {
        UUID reserva = reservar(unico("reciente"));
        jdbc.update("UPDATE reserva SET creado_en = ? WHERE id = ?",
                Timestamp.from(inicio.minus(Duration.ofHours(10))), reserva);

        programador.programar(inicio.minus(Duration.ofHours(5)));

        assertThat(recordatoriosDe(reserva)).isZero();
    }

    @Test
    void unaCitaCanceladaNoRecibeRecordatorio() throws Exception {
        UUID reserva = reservar(unico("cancelada"));
        mvc.perform(lola.propietario().autenticar(patch("/api/panel/reservas/" + reserva + "/estado"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\": \"CANCELADA\"}"))
                .andExpect(status().isOk());

        programador.programar(inicio.minus(Duration.ofHours(23)));

        assertThat(recordatoriosDe(reserva)).isZero();
    }

    // --- Utilidades ---

    private UUID reservar(String email) throws Exception {
        String token = lola.reservarComoCliente(corte, marta, inicio, email);
        return jdbc.queryForObject("SELECT id FROM reserva WHERE token_cancelacion = ?", UUID.class,
                TokenSecreto.hash(token));
    }

    private int recordatoriosDe(UUID reserva) {
        return jdbc.queryForObject("SELECT count(*) FROM email_pendiente WHERE reserva_id = ? AND tipo = 'RECORDATORIO'",
                Integer.class, reserva);
    }

    private List<String> asuntosPara(String email) throws Exception {
        String consulta = URLEncoder.encode("to:\"" + email + "\"", StandardCharsets.UTF_8);
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(mailpit + "/api/v1/search?query=" + consulta))
                .GET().build();
        return JsonPath.read(HTTP.send(peticion, HttpResponse.BodyHandlers.ofString()).body(),
                "$.messages[*].Subject");
    }

    private static String unico(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }
}
