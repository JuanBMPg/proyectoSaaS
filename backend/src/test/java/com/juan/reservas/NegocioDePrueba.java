package com.juan.reservas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/**
 * Monta un negocio con la API del panel, igual que lo haría su propietario, para los tests
 * de la web pública y de las reservas.
 */
public class NegocioDePrueba {

    public static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    private final MockMvc mvc;
    private final SesionDePrueba propietario;

    public NegocioDePrueba(MockMvc mvc, SesionDePrueba propietario) {
        this.mvc = mvc;
        this.propietario = propietario;
    }

    public static NegocioDePrueba registrar(MockMvc mvc) throws Exception {
        return new NegocioDePrueba(mvc, SesionDePrueba.registrarPropietario(mvc));
    }

    public SesionDePrueba propietario() {
        return propietario;
    }

    public String slug() {
        return propietario.slug();
    }

    public String crearServicio(String nombre, int duracionMin, int margenMin, int precioCentimos) throws Exception {
        return crear("servicios", """
                {"nombre": "%s", "duracionMin": %d, "margenMin": %d, "precioCentimos": %d}
                """.formatted(nombre, duracionMin, margenMin, precioCentimos));
    }

    public String crearRecurso(String nombre, String tipo) throws Exception {
        return crear("recursos", "{\"nombre\": \"%s\", \"tipo\": \"%s\"}".formatted(nombre, tipo));
    }

    public void asignar(String recursoId, String... servicioIds) throws Exception {
        mvc.perform(propietario.autenticar(put("/api/panel/recursos/" + recursoId + "/servicios"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"servicioIds\": [\"" + String.join("\", \"", servicioIds) + "\"]}"))
                .andExpect(status().isOk());
    }

    /** Una única franja el día de la semana de {@code fecha}. */
    public void horario(String recursoId, LocalDate fecha, String inicio, String fin) throws Exception {
        mvc.perform(propietario.autenticar(put("/api/panel/recursos/" + recursoId + "/horario"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"franjas\": [{\"diaSemana\": %d, \"horaInicio\": \"%s\", \"horaFin\": \"%s\"}]}"
                                .formatted(fecha.getDayOfWeek().getValue(), inicio, fin)))
                .andExpect(status().isOk());
    }

    /** Instante de una hora local de Madrid en esa fecha. */
    public static Instant instante(LocalDate fecha, String hora) {
        return ZonedDateTime.of(fecha, LocalTime.parse(hora), MADRID).toInstant();
    }

    private String crear(String recurso, String json) throws Exception {
        String respuesta = mvc.perform(propietario.autenticar(post("/api/panel/" + recurso))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }
}
