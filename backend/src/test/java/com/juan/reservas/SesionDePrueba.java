package com.juan.reservas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

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

    /** Añade la cabecera Authorization con el token de esta sesión. */
    public MockHttpServletRequestBuilder autenticar(MockHttpServletRequestBuilder peticion) {
        return peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }
}
