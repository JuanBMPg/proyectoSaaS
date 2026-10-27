package com.juan.reservas.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.juan.reservas.TestDeIntegracion;
import com.juan.reservas.negocio.Negocio;
import com.juan.reservas.negocio.NegocioRepository;

import jakarta.servlet.http.Cookie;

/**
 * Flujo completo de autenticación contra la aplicación real y PostgreSQL:
 * registro, login, acceso al panel, renovación con rotación y cierre de sesión.
 * <p>
 * Sin @Transactional: cada petición confirma sus datos como en producción.
 * Cada test usa un slug y un email únicos para no depender de los demás.
 */
@TestDeIntegracion
class AuthIntegrationTest {

    private static final String PASSWORD = "secreta123";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private NegocioRepository negocioRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private Clock clock;

    private String slug;
    private String email;

    @BeforeEach
    void datosUnicos() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        slug = "lola-" + sufijo;
        email = "lola-" + sufijo + "@example.com";
    }

    // --- Registro ---

    @Test
    void registroDevuelve201ConAccessTokenYRefreshTokenEnCookieSegura() throws Exception {
        mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson(slug, email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/auth")));
    }

    @Test
    void conElTokenDelRegistroSeAccedeAlNegocioPropio() throws Exception {
        String accessToken = accessToken(registrar());

        mvc.perform(get("/api/panel/negocio").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slug))
                .andExpect(jsonPath("$.zonaHoraria").value("Europe/Madrid"))
                .andExpect(jsonPath("$.antelacionMinimaMin").value(120));
    }

    @Test
    void registroConDatosInvalidosDevuelve400ConErroresPorCampo() throws Exception {
        String json = """
                {"nombreNegocio": "Lola", "slug": "Con Espacios", "nombre": "Lola",
                 "email": "no-es-un-email", "password": "corta"}
                """;

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.slug").exists())
                .andExpect(jsonPath("$.errores.email").exists())
                .andExpect(jsonPath("$.errores.password").exists());
    }

    @Test
    void registroConSlugRepetidoDevuelve409() throws Exception {
        registrar();

        mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson(slug, "otro-" + email)))
                .andExpect(status().isConflict());
    }

    // --- Login ---

    @Test
    void loginCorrectoDevuelveTokens() throws Exception {
        registrar();

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email.toUpperCase(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")));
    }

    @Test
    void loginConContraseñaIncorrectaDevuelve401ProblemDetails() throws Exception {
        registrar();

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "incorrecta")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email o contraseña incorrectos"));
    }

    // --- Acceso al panel ---

    @Test
    void panelSinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/panel/negocio"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void panelConTokenManipuladoDevuelve401() throws Exception {
        String accessToken = accessToken(registrar());
        String manipulado = accessToken.substring(0, accessToken.length() - 4) + "AAAA";

        mvc.perform(get("/api/panel/negocio").header(HttpHeaders.AUTHORIZATION, "Bearer " + manipulado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unEmpleadoNoPuedeVerLosDatosDelNegocio() throws Exception {
        registrar();
        Negocio negocio = negocioRepository.findBySlug(slug).orElseThrow();
        String emailEmpleado = "empleado-" + email;
        usuarioRepository.save(new Usuario(negocio, emailEmpleado, passwordEncoder.encode(PASSWORD),
                "Marta", Rol.EMPLEADO, Instant.now(clock)));
        String tokenEmpleado = accessToken(login(emailEmpleado));

        mvc.perform(get("/api/panel/negocio").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenEmpleado))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // --- Refresh y logout ---

    @Test
    void refreshDevuelveTokensNuevosYElRefreshTokenUsadoDejaDeValer() throws Exception {
        Cookie original = refreshCookie(registrar());

        MvcResult renovado = mvc.perform(post("/api/auth/refresh").cookie(original))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        Cookie nueva = refreshCookie(renovado);
        assertThat(nueva.getValue()).isNotEqualTo(original.getValue());

        // Rotación: el token original ya está revocado
        mvc.perform(post("/api/auth/refresh").cookie(original))
                .andExpect(status().isUnauthorized());
        // El nuevo sigue funcionando
        mvc.perform(post("/api/auth/refresh").cookie(nueva))
                .andExpect(status().isOk());
    }

    @Test
    void refreshSinCookieDevuelve401() throws Exception {
        mvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevocaElRefreshTokenYBorraLaCookie() throws Exception {
        Cookie cookie = refreshCookie(registrar());

        mvc.perform(post("/api/auth/logout").cookie(cookie))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        mvc.perform(post("/api/auth/refresh").cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    // --- Utilidades ---

    private MvcResult registrar() throws Exception {
        return mvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson(slug, email)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private MvcResult login(String emailLogin) throws Exception {
        return mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(emailLogin, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private static String accessToken(MvcResult resultado) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.accessToken");
    }

    private static Cookie refreshCookie(MvcResult resultado) {
        Cookie cookie = resultado.getResponse().getCookie(RefreshTokenCookie.NOMBRE);
        assertThat(cookie).as("cookie del refresh token").isNotNull();
        return cookie;
    }

    private static String registroJson(String slug, String email) {
        return """
                {"nombreNegocio": "Peluquería Lola", "slug": "%s", "nombre": "Lola",
                 "email": "%s", "password": "%s"}
                """.formatted(slug, email, PASSWORD);
    }

    private static String loginJson(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }
}
