package com.juan.reservas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

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

    /**
     * Da de alta un empleado vinculado a un recurso (con la API del panel) e inicia su sesión.
     */
    public SesionDePrueba crearEmpleado(String recursoId) throws Exception {
        String email = "empleado-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        mvc.perform(propietario.autenticar(post("/api/panel/empleados"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Empleado", "email": "%s", "password": "%s", "recursoId": "%s"}
                                """.formatted(email, SesionDePrueba.PASSWORD, recursoId)))
                .andExpect(status().isCreated());
        String respuesta = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, SesionDePrueba.PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new SesionDePrueba(JsonPath.read(respuesta, "$.accessToken"), slug(), email);
    }

    /** Reserva desde la web pública (como un cliente) y devuelve el token de su enlace. */
    public String reservarComoCliente(String servicioId, String recursoId, Instant inicio, String email)
            throws Exception {
        String respuesta = mvc.perform(post("/api/publico/" + slug() + "/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"servicioId": "%s", "recursoId": "%s", "inicio": "%s",
                                 "nombre": "Cliente", "email": "%s", "telefono": null}
                                """.formatted(servicioId, recursoId, inicio, email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
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
