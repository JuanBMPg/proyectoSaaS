package com.juan.reservas.reserva;

import static com.juan.reservas.NegocioDePrueba.instante;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.juan.reservas.NegocioDePrueba;
import com.juan.reservas.SesionDePrueba;
import com.juan.reservas.TestDeIntegracion;
import com.juan.reservas.comun.TokenSecreto;

/** Ajustes del negocio y confirmación manual: las reservas llegan pendientes y el negocio decide. */
@TestDeIntegracion
class ConfirmacionManualIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    private NegocioDePrueba lola;
    private LocalDate fecha;
    private String corte;
    private String marta;
    private String pedro;

    @BeforeEach
    void montarNegocio() throws Exception {
        lola = NegocioDePrueba.registrar(mvc);
        fecha = LocalDate.now(NegocioDePrueba.MADRID).plusDays(7);
        corte = lola.crearServicio("Corte", 30, 0, 1500);
        marta = lola.crearRecurso("Marta", "PERSONA");
        pedro = lola.crearRecurso("Pedro", "PERSONA");
        lola.asignar(marta, corte);
        lola.asignar(pedro, corte);
        lola.horario(marta, fecha, "10:00", "12:00");
        lola.horario(pedro, fecha, "10:00", "12:00");
    }

    // --- Ajustes del negocio ---

    @Test
    void elPropietarioCambiaLosDatosYLasReglasDelNegocio() throws Exception {
        ajustes(lola.propietario(), false, 24)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Peluquería Lola"))
                .andExpect(jsonPath("$.telefono").value("600123123"))
                .andExpect(jsonPath("$.direccion").doesNotExist())
                .andExpect(jsonPath("$.etiquetaRecurso").value("Estilista"))
                .andExpect(jsonPath("$.plazoCancelacionHoras").value(24))
                .andExpect(jsonPath("$.confirmacionAutomatica").value(false))
                .andExpect(jsonPath("$.slug").value(lola.slug()));

        mvc.perform(lola.propietario().autenticar(get("/api/panel/negocio")))
                .andExpect(jsonPath("$.antelacionMaximaDias").value(60));
        mvc.perform(get("/api/publico/" + lola.slug()))
                .andExpect(jsonPath("$.nombre").value("Peluquería Lola"))
                .andExpect(jsonPath("$.etiquetaRecurso").value("Estilista"));
    }

    @Test
    void rechazaReglasFueraDeRango() throws Exception {
        mvc.perform(lola.propietario().autenticar(put("/api/panel/negocio"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": " ", "email": "no-es-un-email", "etiquetaRecurso": "Profesional",
                                 "antelacionMinimaMin": -1, "antelacionMaximaDias": 0,
                                 "plazoCancelacionHoras": 1000, "confirmacionAutomatica": null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.email").exists())
                .andExpect(jsonPath("$.errores.antelacionMinimaMin").exists())
                .andExpect(jsonPath("$.errores.antelacionMaximaDias").exists())
                .andExpect(jsonPath("$.errores.plazoCancelacionHoras").exists())
                .andExpect(jsonPath("$.errores.confirmacionAutomatica").exists());
    }

    @Test
    void unEmpleadoNoPuedeCambiarLosAjustes() throws Exception {
        SesionDePrueba empleado = lola.crearEmpleado(marta);

        ajustes(empleado, false, 24).andExpect(status().isForbidden());
    }

    // --- Confirmación manual ---

    @Test
    void conConfirmacionManualLaReservaLlegaPendienteYBloqueaElHueco() throws Exception {
        ajustes(lola.propietario(), false, 24).andExpect(status().isOk());

        String token = lola.reservarComoCliente(corte, marta, instante(fecha, "10:00"), unico());

        mvc.perform(get("/api/publico/reservas/" + token)).andExpect(jsonPath("$.estado").value("PENDIENTE"));
        // Mientras el negocio decide, nadie más puede quedarse esa hora con Marta
        mvc.perform(get("/api/publico/" + lola.slug() + "/disponibilidad")
                        .param("servicioId", corte).param("fecha", fecha.toString()).param("recursoId", marta))
                .andExpect(jsonPath("$.huecos[0].inicio").value(instante(fecha, "10:30").toString()));
    }

    @Test
    void lasSolicitudesPendientesSeListanEnOrdenYCadaEmpleadoVeLasSuyas() throws Exception {
        ajustes(lola.propietario(), false, 24).andExpect(status().isOk());
        lola.reservarComoCliente(corte, pedro, instante(fecha, "11:00"), unico());
        lola.reservarComoCliente(corte, marta, instante(fecha, "10:00"), unico());
        SesionDePrueba empleadaMarta = lola.crearEmpleado(marta);

        pendientes(lola.propietario())
                .andExpect(jsonPath("$[*].recurso.nombre", contains("Marta", "Pedro")));
        pendientes(empleadaMarta)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].recurso.nombre").value("Marta"));
    }

    @Test
    void alConfirmarlaElClienteRecibeUnEmailYDejaDeEstarPendiente() throws Exception {
        ajustes(lola.propietario(), false, 24).andExpect(status().isOk());
        String token = lola.reservarComoCliente(corte, marta, instante(fecha, "10:00"), unico());

        cambiarEstado(token, "CONFIRMADA").andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        pendientes(lola.propietario()).andExpect(jsonPath("$", hasSize(0)));
        assertThat(asuntos(token)).satisfiesExactly(
                creada -> assertThat(creada).startsWith("Tu cita en "),
                confirmada -> assertThat(confirmada).startsWith("Cita confirmada: "));
    }

    @Test
    void alRechazarlaElEmailDiceQueNoSeAceptoYOfreceOtraHora() throws Exception {
        ajustes(lola.propietario(), false, 24).andExpect(status().isOk());
        String token = lola.reservarComoCliente(corte, marta, instante(fecha, "10:00"), unico());

        cambiarEstado(token, "CANCELADA").andExpect(jsonPath("$.estado").value("CANCELADA"));

        assertThat(asuntos(token)).satisfiesExactly(
                creada -> assertThat(creada).startsWith("Tu cita en "),
                rechazada -> assertThat(rechazada).startsWith("Solicitud no aceptada: "));
    }

    @Test
    void conConfirmacionAutomaticaLaReservaNaceConfirmadaYNoHayPendientes() throws Exception {
        String token = lola.reservarComoCliente(corte, marta, instante(fecha, "10:00"), unico());

        mvc.perform(get("/api/publico/reservas/" + token)).andExpect(jsonPath("$.estado").value("CONFIRMADA"));
        pendientes(lola.propietario()).andExpect(jsonPath("$", hasSize(0)));
    }

    // --- Utilidades ---

    private ResultActions ajustes(SesionDePrueba sesion, boolean confirmacionAutomatica, int plazo) throws Exception {
        return mvc.perform(sesion.autenticar(put("/api/panel/negocio"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre": " Peluquería Lola ", "email": "hola@lola.example", "telefono": "600123123",
                         "direccion": "", "etiquetaRecurso": "Estilista", "antelacionMinimaMin": 60,
                         "antelacionMaximaDias": 60, "plazoCancelacionHoras": %d, "confirmacionAutomatica": %s}
                        """.formatted(plazo, confirmacionAutomatica)));
    }

    private ResultActions pendientes(SesionDePrueba sesion) throws Exception {
        return mvc.perform(sesion.autenticar(get("/api/panel/reservas/pendientes"))).andExpect(status().isOk());
    }

    private ResultActions cambiarEstado(String token, String estado) throws Exception {
        return mvc.perform(lola.propietario().autenticar(patch("/api/panel/reservas/" + reservaId(token) + "/estado"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\": \"" + estado + "\"}"))
                .andExpect(status().isOk());
    }

    private UUID reservaId(String token) {
        return jdbc.queryForObject("SELECT id FROM reserva WHERE token_cancelacion = ?", UUID.class,
                TokenSecreto.hash(token));
    }

    /** Asuntos de los emails de la reserva, en orden. */
    private List<String> asuntos(String token) {
        return jdbc.queryForList("SELECT asunto FROM email_pendiente WHERE reserva_id = ? ORDER BY creado_en",
                String.class, reservaId(token));
    }

    private static String unico() {
        return "cliente-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }
}
