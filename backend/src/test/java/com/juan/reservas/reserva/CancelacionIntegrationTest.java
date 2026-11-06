package com.juan.reservas.reserva;

import static com.juan.reservas.NegocioDePrueba.instante;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.juan.reservas.NegocioDePrueba;
import com.juan.reservas.TestDeIntegracion;
import com.juan.reservas.comun.TokenSecreto;

/** Cancelación desde el enlace del cliente y desde el panel, con su email en la cola. */
@TestDeIntegracion
class CancelacionIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    private NegocioDePrueba lola;
    private LocalDate fecha;
    private Instant inicio;
    private String corte;
    private String marta;

    @BeforeEach
    void montarNegocio() throws Exception {
        lola = NegocioDePrueba.registrar(mvc);
        fecha = LocalDate.now(NegocioDePrueba.MADRID).plusDays(7);
        corte = lola.crearServicio("Corte", 30, 0, 1500);
        marta = lola.crearRecurso("Marta", "PERSONA");
        lola.asignar(marta, corte);
        lola.horario(marta, fecha, "10:00", "12:00");
        inicio = instante(fecha, "10:00");
    }

    @Test
    void laReservaDiceHastaCuandoSePuedeCancelar() throws Exception {
        String token = reservar();

        mvc.perform(get("/api/publico/reservas/" + token))
                .andExpect(jsonPath("$.cancelable").value(true))
                .andExpect(jsonPath("$.cancelableHasta").value(inicio.minus(Duration.ofHours(24)).toString()));
    }

    @Test
    void elClienteCancelaDentroDelPlazoYElHuecoQuedaLibre() throws Exception {
        String token = reservar();

        cancelar(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.cancelable").value(false))
                .andExpect(jsonPath("$.cancelableHasta").doesNotExist());

        mvc.perform(get("/api/publico/" + lola.slug() + "/disponibilidad")
                        .param("servicioId", corte).param("fecha", fecha.toString()))
                .andExpect(jsonPath("$.huecos[0].inicio").value(inicio.toString()));
        assertThat(emailsDe(token, "RESERVA_CANCELADA")).isEqualTo(1);
    }

    @Test
    void fueraDelPlazoNoSePuedeCancelarYSeExplicaComo() throws Exception {
        String token = reservar();
        // Plazo de 10 días: la cita, dentro de 7, ya está fuera
        jdbc.update("UPDATE negocio SET plazo_cancelacion_horas = 240, telefono = '600123123' WHERE slug = ?",
                lola.slug());

        mvc.perform(get("/api/publico/reservas/" + token)).andExpect(jsonPath("$.cancelable").value(false));
        cancelar(token)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(startsWith(
                        "Las citas se pueden cancelar online hasta 240 horas antes. Si no puedes ir, llama a ")))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.endsWith(" al 600123123.")));
        assertThat(emailsDe(token, "RESERVA_CANCELADA")).isZero();
    }

    @Test
    void unaReservaCanceladaNoSeVuelveACancelar() throws Exception {
        String token = reservar();
        cancelar(token).andExpect(status().isOk());

        cancelar(token)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Esta reserva ya no se puede cancelar: está cancelada"));
    }

    @Test
    void unEnlaceQueNoExisteDa404() throws Exception {
        cancelar("no-existe").andExpect(status().isNotFound());
    }

    @Test
    void siCancelaElNegocioElClienteTambienRecibeEmail() throws Exception {
        String token = reservar();
        UUID id = reservaId(token);

        mvc.perform(lola.propietario().autenticar(patch("/api/panel/reservas/" + id + "/estado"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\": \"CANCELADA\"}"))
                .andExpect(status().isOk());

        assertThat(emailsDe(token, "RESERVA_CANCELADA")).isEqualTo(1);
    }

    @Test
    void unDobleClicEnCancelarSoloCancelaUnaVezYEnviaUnEmail() throws Exception {
        String token = reservar();
        int peticiones = 5;
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(peticiones);
        List<Future<Integer>> estados = new ArrayList<>();
        for (int i = 0; i < peticiones; i++) {
            estados.add(hilos.submit(() -> {
                salida.await();
                return cancelar(token).andReturn().getResponse().getStatus();
            }));
        }
        salida.countDown();
        hilos.shutdown();
        assertThat(hilos.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        List<Integer> respuestas = new ArrayList<>();
        for (Future<Integer> estado : estados) {
            respuestas.add(estado.get());
        }
        assertThat(respuestas).filteredOn(codigo -> codigo == 200).hasSize(1);
        assertThat(respuestas).filteredOn(codigo -> codigo == 422).hasSize(peticiones - 1);
        assertThat(emailsDe(token, "RESERVA_CANCELADA")).isEqualTo(1);
    }

    @Test
    void laCancelacionNoNecesitaLoginPeroElPanelSi() throws Exception {
        String token = reservar();

        mvc.perform(patch("/api/panel/reservas/" + reservaId(token) + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\": \"CANCELADA\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/panel/reservas").param("desde", inicio.minusSeconds(3600).toString())
                        .param("hasta", inicio.plusSeconds(3600).toString()))
                .andExpect(status().isUnauthorized());
        mvc.perform(lola.propietario().autenticar(get("/api/panel/reservas"))
                        .param("desde", inicio.minusSeconds(3600).toString())
                        .param("hasta", inicio.plusSeconds(3600).toString()))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    // --- Utilidades ---

    private String reservar() throws Exception {
        return lola.reservarComoCliente(corte, marta, inicio,
                "cliente-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
    }

    private ResultActions cancelar(String token) throws Exception {
        return mvc.perform(post("/api/publico/reservas/" + token + "/cancelar"));
    }

    private UUID reservaId(String token) {
        return jdbc.queryForObject("SELECT id FROM reserva WHERE token_cancelacion = ?", UUID.class,
                TokenSecreto.hash(token));
    }

    private int emailsDe(String token, String tipo) {
        return jdbc.queryForObject("SELECT count(*) FROM email_pendiente WHERE reserva_id = ? AND tipo = ?",
                Integer.class, reservaId(token), tipo);
    }
}
