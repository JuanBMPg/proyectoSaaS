package com.juan.reservas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.juan.reservas.auth.Rol;
import com.juan.reservas.auth.Usuario;
import com.juan.reservas.auth.UsuarioRepository;
import com.juan.reservas.negocio.NegocioRepository;

/**
 * Propietario de un negocio nuevo, registrado a través de la API, para los tests de integración.
 * Cada llamada crea un negocio distinto, así los tests no dependen unos de otros.
 */
public record SesionDePrueba(String accessToken, String slug, String email) {

    public static final String PASSWORD = "secreta123";

    public static SesionDePrueba registrarPropietario(MockMvc mvc) throws Exception {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        String slug = "negocio-" + sufijo;
        String email = "propietario-" + sufijo + "@example.com";
        String json = """
                {"nombreNegocio": "Negocio %s", "slug": "%s", "nombre": "Propietario",
                 "email": "%s", "password": "%s"}
                """.formatted(sufijo, slug, email, PASSWORD);

        String respuesta = mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new SesionDePrueba(JsonPath.read(respuesta, "$.accessToken"), slug, email);
    }

    /**
     * Crea un empleado en el negocio de este propietario e inicia su sesión. Se guarda
     * directamente en la BD porque la invitación de empleados llega en una tarea posterior.
     */
    public SesionDePrueba crearEmpleado(MockMvc mvc, NegocioRepository negocios, UsuarioRepository usuarios,
            PasswordEncoder passwordEncoder, Clock clock) throws Exception {
        String emailEmpleado = "empleado-" + email;
        usuarios.save(new Usuario(negocios.findBySlug(slug).orElseThrow(), emailEmpleado,
                passwordEncoder.encode(PASSWORD), "Empleado", Rol.EMPLEADO, Instant.now(clock)));

        String respuesta = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(emailEmpleado, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new SesionDePrueba(JsonPath.read(respuesta, "$.accessToken"), slug, emailEmpleado);
    }

    /** Añade la cabecera Authorization con el token de esta sesión. */
    public MockHttpServletRequestBuilder autenticar(MockHttpServletRequestBuilder peticion) {
        return peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }
}
