package com.juan.reservas.reserva;

import static com.juan.reservas.NegocioDePrueba.instante;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.juan.reservas.NegocioDePrueba;
import com.juan.reservas.TestDeIntegracion;

/**
 * El test que pide el documento: varias personas reservan el mismo hueco en el mismo instante
 * y solo una se lo queda. Cada petición va en su propio hilo y en su propia transacción.
 * <p>
 * Algunas fallarán en la comprobación de disponibilidad (si llegan cuando la primera ya ha
 * confirmado) y otras en el INSERT, por la restricción reserva_sin_solape (si comprobaron a la
 * vez). Por cualquiera de los dos caminos, el resultado tiene que ser siempre el mismo.
 */
@TestDeIntegracion
class ReservaConcurrenteIntegrationTest {

    private static final int PETICIONES = 10;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void diezPeticionesAlMismoHuecoGuardanUnaSolaReserva() throws Exception {
        NegocioDePrueba lola = NegocioDePrueba.registrar(mvc);
        LocalDate fecha = LocalDate.now(NegocioDePrueba.MADRID).plusDays(7);
        String corte = lola.crearServicio("Corte", 30, 10, 1500);
        String marta = lola.crearRecurso("Marta", "PERSONA");
        lola.asignar(marta, corte);
        lola.horario(marta, fecha, "10:00", "12:00");
        Instant hueco = instante(fecha, "10:00");

        // Todos los hilos esperan en la salida y arrancan a la vez al bajar la barrera
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(PETICIONES);
        List<Future<Integer>> estados = new ArrayList<>();
        for (int i = 0; i < PETICIONES; i++) {
            String email = "cliente" + i + "@example.com";
            estados.add(hilos.submit(() -> {
                salida.await();
                return mvc.perform(post("/api/publico/" + lola.slug() + "/reservas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(ReservaPublicaIntegrationTest.json(corte, marta, hueco, email, "Cliente", null)))
                        .andReturn().getResponse().getStatus();
            }));
        }
        salida.countDown();
        hilos.shutdown();
        assertThat(hilos.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        List<Integer> respuestas = new ArrayList<>();
        for (Future<Integer> estado : estados) {
            respuestas.add(estado.get());
        }

        assertThat(respuestas).filteredOn(codigo -> codigo == 201).hasSize(1);
        assertThat(respuestas).filteredOn(codigo -> codigo == 409).hasSize(PETICIONES - 1);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM reserva WHERE recurso_id = ?::uuid AND estado IN ('PENDIENTE', 'CONFIRMADA')",
                Integer.class, marta)).isEqualTo(1);
    }
}
