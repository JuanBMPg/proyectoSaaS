package com.juan.reservas.reserva;

import static com.juan.reservas.NegocioDePrueba.instante;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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

/** Agenda del panel: lectura, reservas manuales y cambios de estado, con las reglas de cada rol. */
@TestDeIntegracion
class ReservaPanelIntegrationTest {

    private static final String RUTA = "/api/panel/reservas";

    @Autowired
    private MockMvc mvc;

    private NegocioDePrueba lola;
    private LocalDate fecha;
    private String corte;
    private String marta;
    private String pedro;

    @BeforeEach
    void montarNegocioConDosReservas() throws Exception {
        lola = NegocioDePrueba.registrar(mvc);
        fecha = LocalDate.now(NegocioDePrueba.MADRID).plusDays(7);
        corte = lola.crearServicio("Corte", 30, 0, 1500);
        marta = lola.crearRecurso("Marta", "PERSONA");
        pedro = lola.crearRecurso("Pedro", "PERSONA");
        for (String recurso : new String[] {marta, pedro}) {
            lola.asignar(recurso, corte);
            lola.horario(recurso, fecha, "10:00", "12:00");
        }
        lola.reservarComoCliente(corte, marta, instante(fecha, "10:00"), "ana@example.com");
        lola.reservarComoCliente(corte, pedro, instante(fecha, "10:30"), "luis@example.com");
    }

    // --- Lectura ---

    @Test
    void elPropietarioVeTodasLasReservasDelDiaConLosDatosDelCliente() throws Exception {
        agenda(lola.propietario(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].recurso.nombre", contains("Marta", "Pedro")))
                .andExpect(jsonPath("$[0].cliente.email").value("ana@example.com"))
                .andExpect(jsonPath("$[0].servicio.nombre").value("Corte"))
                .andExpect(jsonPath("$[0].estado").value("CONFIRMADA"));
    }

    @Test
    void elPropietarioPuedeFiltrarPorRecurso() throws Exception {
        agenda(lola.propietario(), pedro)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].recurso.nombre").value("Pedro"));
    }

    @Test
    void unEmpleadoSoloVeLasDeSuRecursoAunquePidaOtro() throws Exception {
        SesionDePrueba empleadaMarta = lola.crearEmpleado(marta);

        agenda(empleadaMarta, pedro)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].recurso.nombre", everyItem(is("Marta"))));
    }

    @Test
    void otroNegocioNoVeNada() throws Exception {
        NegocioDePrueba ruiz = NegocioDePrueba.registrar(mvc);

        agenda(ruiz.propietario(), null).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void unRangoInvertidoOMayorDe62DiasDevuelve400() throws Exception {
        Instant inicio = instante(fecha, "00:00");
        mvc.perform(lola.propietario().autenticar(get(RUTA))
                        .param("desde", inicio.toString()).param("hasta", inicio.minusSeconds(60).toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(lola.propietario().autenticar(get(RUTA))
                        .param("desde", inicio.toString()).param("hasta", inicio.plusSeconds(63L * 24 * 3600).toString()))
                .andExpect(status().isBadRequest());
    }

    // --- Cambios de estado ---

    @Test
    void completarUnaCitaYNoPoderCancelarlaDespues() throws Exception {
        String id = idDeLaReservaDe("Marta");

        cambiarEstado(lola.propietario(), id, "COMPLETADA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"));
        cambiarEstado(lola.propietario(), id, "CANCELADA")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Una reserva completada no puede pasar a cancelada"));
    }

    @Test
    void cancelarLiberaElHuecoParaOtroCliente() throws Exception {
        cambiarEstado(lola.propietario(), idDeLaReservaDe("Marta"), "CANCELADA").andExpect(status().isOk());

        lola.reservarComoCliente(corte, marta, instante(fecha, "10:00"), "otra@example.com");
    }

    @Test
    void unEmpleadoGestionaLasDeSuRecursoPeroNoLasDeOtro() throws Exception {
        SesionDePrueba empleadaMarta = lola.crearEmpleado(marta);

        cambiarEstado(empleadaMarta, idDeLaReservaDe("Marta"), "NO_PRESENTADO").andExpect(status().isOk());
        cambiarEstado(empleadaMarta, idDeLaReservaDe("Pedro"), "CANCELADA").andExpect(status().isNotFound());
    }

    @Test
    void otroNegocioRecibe404AlCambiarElEstado() throws Exception {
        NegocioDePrueba ruiz = NegocioDePrueba.registrar(mvc);

        cambiarEstado(ruiz.propietario(), idDeLaReservaDe("Marta"), "CANCELADA").andExpect(status().isNotFound());
    }

    // --- Reserva manual ---

    @Test
    void elNegocioPuedeReservarFueraDeHorarioPeroNoSolapar() throws Exception {
        reservaManual(lola.propietario(), marta, instante(fecha, "18:00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.recurso.nombre").value("Marta"));

        reservaManual(lola.propietario(), marta, instante(fecha, "10:15"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ese hueco acaba de ocuparse"));
    }

    @Test
    void unEmpleadoReservaParaSuRecursoPeroNoParaOtro() throws Exception {
        SesionDePrueba empleadaMarta = lola.crearEmpleado(marta);

        reservaManual(empleadaMarta, marta, instante(fecha, "11:00")).andExpect(status().isCreated());
        reservaManual(empleadaMarta, pedro, instante(fecha, "11:30")).andExpect(status().isNotFound());
    }

    // --- Utilidades ---

    private ResultActions agenda(SesionDePrueba sesion, String recursoId) throws Exception {
        var peticion = sesion.autenticar(get(RUTA))
                .param("desde", instante(fecha, "00:00").toString())
                .param("hasta", instante(fecha.plusDays(1), "00:00").toString());
        if (recursoId != null) {
            peticion.param("recursoId", recursoId);
        }
        return mvc.perform(peticion);
    }

    private String idDeLaReservaDe(String recurso) throws Exception {
        String respuesta = agenda(lola.propietario(), null).andReturn().getResponse().getContentAsString();
        return JsonPath.<List<String>>read(respuesta, "$[?(@.recurso.nombre == '%s')].id".formatted(recurso))
                .getFirst();
    }

    private ResultActions cambiarEstado(SesionDePrueba sesion, String id, String estado) throws Exception {
        return mvc.perform(sesion.autenticar(patch(RUTA + "/" + id + "/estado"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\": \"" + estado + "\"}"));
    }

    private ResultActions reservaManual(SesionDePrueba sesion, String recursoId, Instant inicio) throws Exception {
        return mvc.perform(sesion.autenticar(post(RUTA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"servicioId": "%s", "recursoId": "%s", "inicio": "%s",
                         "nombre": "Por teléfono", "email": "telefono@example.com", "telefono": "600111222"}
                        """.formatted(corte, recursoId, inicio)));
    }
}
