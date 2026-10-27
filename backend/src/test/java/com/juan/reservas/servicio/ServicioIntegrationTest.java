package com.juan.reservas.servicio;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.juan.reservas.SesionDePrueba;
import com.juan.reservas.TestDeIntegracion;
import com.juan.reservas.auth.Rol;
import com.juan.reservas.auth.Usuario;
import com.juan.reservas.auth.UsuarioRepository;
import com.juan.reservas.negocio.NegocioRepository;

/** CRUD de /api/panel/servicios contra la aplicación real, incluido el aislamiento entre negocios. */
@TestDeIntegracion
class ServicioIntegrationTest {

    private static final String RUTA = "/api/panel/servicios";

    @Autowired
    private MockMvc mvc;

    private SesionDePrueba lola;

    @BeforeEach
    void registrarNegocio() throws Exception {
        lola = SesionDePrueba.registrarPropietario(mvc);
    }

    @Test
    void creaUnServicioYApareceEnElListado() throws Exception {
        mvc.perform(lola.autenticar(post(RUTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(servicioJson("Corte de pelo", 30, 10, 1500)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nombre").value("Corte de pelo"))
                .andExpect(jsonPath("$.duracionMin").value(30))
                .andExpect(jsonPath("$.margenMin").value(10))
                .andExpect(jsonPath("$.precioCentimos").value(1500))
                .andExpect(jsonPath("$.activo").value(true));

        mvc.perform(lola.autenticar(get(RUTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre").value(hasItem("Corte de pelo")));
    }

    @Test
    void validaLosDatosDelServicio() throws Exception {
        mvc.perform(lola.autenticar(post(RUTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(servicioJson("", 0, -5, -1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.duracionMin").exists())
                .andExpect(jsonPath("$.errores.margenMin").exists())
                .andExpect(jsonPath("$.errores.precioCentimos").exists());
    }

    @Test
    void editaUnServicio() throws Exception {
        String id = crearServicio(lola, "Corte");

        mvc.perform(lola.autenticar(put(RUTA + "/" + id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(servicioJson("Corte y peinado", 45, 0, 2500)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Corte y peinado"))
                .andExpect(jsonPath("$.duracionMin").value(45))
                .andExpect(jsonPath("$.precioCentimos").value(2500))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void desactivarEsIdempotenteYElServicioSePuedeReactivar() throws Exception {
        String id = crearServicio(lola, "Mechas");

        mvc.perform(lola.autenticar(delete(RUTA + "/" + id))).andExpect(status().isNoContent());
        mvc.perform(lola.autenticar(delete(RUTA + "/" + id))).andExpect(status().isNoContent());

        mvc.perform(lola.autenticar(get(RUTA)))
                .andExpect(jsonPath("$[?(@.id == '%s')].activo".formatted(id)).value(false));

        mvc.perform(lola.autenticar(put(RUTA + "/" + id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Mechas", "duracionMin": 90, "margenMin": 0,
                                 "precioCentimos": 4000, "activo": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void otroNegocioNoVeNiPuedeTocarElServicio() throws Exception {
        String id = crearServicio(lola, "Corte");
        SesionDePrueba ruiz = SesionDePrueba.registrarPropietario(mvc);

        mvc.perform(ruiz.autenticar(get(RUTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(not(hasItem(id))));
        mvc.perform(ruiz.autenticar(put(RUTA + "/" + id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(servicioJson("Robado", 30, 0, 0)))
                .andExpect(status().isNotFound());
        mvc.perform(ruiz.autenticar(delete(RUTA + "/" + id)))
                .andExpect(status().isNotFound());

        // El servicio de Lola sigue intacto
        mvc.perform(lola.autenticar(get(RUTA)))
                .andExpect(jsonPath("$[?(@.id == '%s')].nombre".formatted(id)).value("Corte"))
                .andExpect(jsonPath("$[?(@.id == '%s')].activo".formatted(id)).value(true));
    }

    @Test
    void unEmpleadoNoPuedeGestionarServicios(
            @Autowired NegocioRepository negocios, @Autowired UsuarioRepository usuarios,
            @Autowired PasswordEncoder passwordEncoder, @Autowired Clock clock) throws Exception {
        String emailEmpleado = "empleado-" + lola.email();
        usuarios.save(new Usuario(negocios.findBySlug(lola.slug()).orElseThrow(), emailEmpleado,
                passwordEncoder.encode(SesionDePrueba.PASSWORD), "Marta", Rol.EMPLEADO, Instant.now(clock)));
        String respuesta = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(emailEmpleado, SesionDePrueba.PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        SesionDePrueba empleado = new SesionDePrueba(JsonPath.read(respuesta, "$.accessToken"), lola.slug(),
                emailEmpleado);

        mvc.perform(empleado.autenticar(get(RUTA))).andExpect(status().isForbidden());
        mvc.perform(empleado.autenticar(post(RUTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(servicioJson("Corte", 30, 0, 0)))
                .andExpect(status().isForbidden());
    }

    private String crearServicio(SesionDePrueba sesion, String nombre) throws Exception {
        String respuesta = mvc.perform(sesion.autenticar(post(RUTA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(servicioJson(nombre, 30, 0, 1500)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    static String servicioJson(String nombre, int duracionMin, int margenMin, int precioCentimos) {
        return """
                {"nombre": "%s", "duracionMin": %d, "margenMin": %d, "precioCentimos": %d}
                """.formatted(nombre, duracionMin, margenMin, precioCentimos);
    }
}
