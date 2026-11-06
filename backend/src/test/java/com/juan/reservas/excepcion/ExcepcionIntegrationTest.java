package com.juan.reservas.excepcion;

import static com.juan.reservas.NegocioDePrueba.instante;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.juan.reservas.NegocioDePrueba;
import com.juan.reservas.SesionDePrueba;
import com.juan.reservas.TestDeIntegracion;

/**
 * Excepciones del horario: la API del panel y su efecto en los huecos de la web pública.
 * Dos profesionales con el mismo horario (10:00-12:00, servicio de 30 min sin margen y rejilla
 * de 15 min: 7 huecos cada uno) para ver que una excepción de un recurso no afecta al otro.
 */
@TestDeIntegracion
class ExcepcionIntegrationTest {

    @Autowired
    private MockMvc mvc;

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

    // --- Efecto en la disponibilidad ---

    @Test
    void unFestivoDelNegocioDejaElDiaSinHuecos() throws Exception {
        crear(null, fecha, fecha, null, null, "Festivo local").andExpect(status().isCreated());

        huecos().andExpect(jsonPath("$.huecos", hasSize(0)));
    }

    @Test
    void lasVacacionesDeUnRecursoSoloQuitanSusHuecos() throws Exception {
        crear(marta, fecha.minusDays(2), fecha.plusDays(2), null, null, "Vacaciones").andExpect(status().isCreated());

        huecos()
                .andExpect(jsonPath("$.huecos", hasSize(7)))
                .andExpect(jsonPath("$.huecos[*].recursoId", everyItem(is(pedro))));
    }

    @Test
    void unHorarioEspecialSustituyeAlSemanal() throws Exception {
        crear(null, fecha, fecha, "11:00", "13:00", "Horario de feria").andExpect(status().isCreated());

        // 11:00-13:00 para los dos: de 11:00 a 12:30 cada 15 min, 7 huecos cada uno
        huecos()
                .andExpect(jsonPath("$.huecos", hasSize(14)))
                .andExpect(jsonPath("$.huecos[0].inicio").value(instante(fecha, "11:00").toString()));
    }

    @Test
    void elHorarioEspecialDelRecursoGanaAlDelNegocioYCerrarGanaAAbrir() throws Exception {
        crear(null, fecha, fecha, "09:00", "13:00", "Víspera de festivo").andExpect(status().isCreated());
        crear(marta, fecha, fecha, "10:00", "11:00", "Solo por la mañana").andExpect(status().isCreated());
        crear(pedro, fecha, fecha, null, null, "Médico").andExpect(status().isCreated());

        // Marta: 10:00, 10:15 y 10:30 (su horario especial). Pedro: nada (su cierre gana)
        huecos()
                .andExpect(jsonPath("$.huecos[*].recursoId", everyItem(is(marta))))
                .andExpect(jsonPath("$.huecos[*].inicio", contains(instante(fecha, "10:00").toString(),
                        instante(fecha, "10:15").toString(), instante(fecha, "10:30").toString())));
    }

    @Test
    void noSePuedeReservarUnDiaCerradoAunqueSeLlameALaApiDirectamente() throws Exception {
        crear(null, fecha, fecha, null, null, "Festivo").andExpect(status().isCreated());

        mvc.perform(post("/api/publico/" + lola.slug() + "/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"servicioId": "%s", "recursoId": "%s", "inicio": "%s",
                                 "nombre": "Juan", "email": "juan@example.com"}
                                """.formatted(corte, marta, instante(fecha, "10:00"))))
                .andExpect(status().isConflict());
    }

    @Test
    void alBorrarLaExcepcionVuelveElHorarioSemanal() throws Exception {
        String respuesta = crear(null, fecha, fecha, null, null, "Festivo")
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(respuesta, "$.excepcion.id");

        mvc.perform(lola.propietario().autenticar(delete("/api/panel/excepciones/" + id)))
                .andExpect(status().isNoContent());

        huecos().andExpect(jsonPath("$.huecos", hasSize(14)));
    }

    // --- API del panel ---

    @Test
    void alCrearlaDevuelveLaExcepcionYCuantasReservasActivasHayEsosDias() throws Exception {
        lola.reservarComoCliente(corte, marta, instante(fecha, "10:00"), "uno@example.com");
        lola.reservarComoCliente(corte, pedro, instante(fecha, "11:00"), "dos@example.com");

        crear(marta, fecha, fecha, null, null, "Vacaciones")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.excepcion.recurso.nombre").value("Marta"))
                .andExpect(jsonPath("$.excepcion.fechaInicio").value(fecha.toString()))
                .andExpect(jsonPath("$.excepcion.horaInicio").doesNotExist())
                .andExpect(jsonPath("$.excepcion.motivo").value("Vacaciones"))
                .andExpect(jsonPath("$.reservasActivas").value(1));

        crear(null, fecha, fecha, "10:00", "11:00", "Feria")
                .andExpect(jsonPath("$.excepcion.recurso").doesNotExist())
                .andExpect(jsonPath("$.excepcion.horaInicio").value("10:00"))
                .andExpect(jsonPath("$.reservasActivas").value(2));
    }

    @Test
    void elListadoTieneLasVigentesYFuturasOrdenadasPorFecha() throws Exception {
        crear(null, fecha.plusDays(10), fecha.plusDays(10), null, null, "Después").andExpect(status().isCreated());
        crear(marta, fecha, fecha.plusDays(3), null, null, "Antes").andExpect(status().isCreated());
        crear(null, fecha.minusDays(30), fecha.minusDays(20), null, null, "Pasada").andExpect(status().isCreated());

        mvc.perform(lola.propietario().autenticar(get("/api/panel/excepciones")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].motivo", contains("Antes", "Después")));

        mvc.perform(lola.propietario().autenticar(get("/api/panel/excepciones")
                        .param("desde", fecha.minusDays(25).toString())))
                .andExpect(jsonPath("$[*].motivo", contains("Pasada", "Antes", "Después")));
    }

    @Test
    void rechazaFechasInvertidasHorasIncompletasYHorasInvertidas() throws Exception {
        crear(null, fecha, fecha.minusDays(1), null, null, "Mal")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fechasEnOrden").exists());
        crear(null, fecha, fecha, "10:00", null, "Mal")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.horasCoherentes").exists());
        crear(null, fecha, fecha, "12:00", "10:00", "Mal")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.horasCoherentes").exists());
        crear(null, fecha, fecha.plusDays(400), null, null, "Mal")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.duracionRazonable").exists());
        crear(null, fecha, fecha, null, null, " ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivo").exists());
    }

    @Test
    void otroNegocioNoVeNiBorraLasExcepcionesNiUsaSusRecursos() throws Exception {
        String respuesta = crear(null, fecha, fecha, null, null, "Festivo")
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(respuesta, "$.excepcion.id");
        SesionDePrueba otro = SesionDePrueba.registrarPropietario(mvc);

        mvc.perform(otro.autenticar(get("/api/panel/excepciones")))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(otro.autenticar(delete("/api/panel/excepciones/" + id)))
                .andExpect(status().isNotFound());
        crearComo(otro, marta, fecha, fecha, null, null, "Ajeno")
                .andExpect(status().isNotFound());
    }

    @Test
    void unEmpleadoNoPuedeGestionarExcepciones() throws Exception {
        SesionDePrueba empleado = lola.crearEmpleado(marta);

        mvc.perform(empleado.autenticar(get("/api/panel/excepciones")))
                .andExpect(status().isForbidden());
        crearComo(empleado, marta, fecha, fecha, null, null, "Vacaciones")
                .andExpect(status().isForbidden());
    }

    // --- Utilidades ---

    private ResultActions huecos() throws Exception {
        return mvc.perform(get("/api/publico/" + lola.slug() + "/disponibilidad")
                        .param("servicioId", corte)
                        .param("fecha", fecha.toString()))
                .andExpect(status().isOk());
    }

    private ResultActions crear(String recursoId, LocalDate desde, LocalDate hasta, String horaInicio,
            String horaFin, String motivo) throws Exception {
        return crearComo(lola.propietario(), recursoId, desde, hasta, horaInicio, horaFin, motivo);
    }

    private ResultActions crearComo(SesionDePrueba sesion, String recursoId, LocalDate desde, LocalDate hasta,
            String horaInicio, String horaFin, String motivo) throws Exception {
        return mvc.perform(sesion.autenticar(post("/api/panel/excepciones"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"recursoId": %s, "fechaInicio": "%s", "fechaFin": "%s",
                         "horaInicio": %s, "horaFin": %s, "motivo": "%s"}
                        """.formatted(json(recursoId), desde, hasta, json(horaInicio), json(horaFin), motivo)));
    }

    private static String json(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }
}
