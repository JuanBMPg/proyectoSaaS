package com.juan.reservas.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.juan.reservas.TestDeIntegracion;

/** El negocio de demostración se crea completo, se puede usar desde la web y no se duplica. */
@TestDeIntegracion
class DatosDemoIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private DatosDemo datos;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void creaUnNegocioUsableYNoLoDuplica() throws Exception {
        assertThat(datos.cargar()).isTrue();

        // Página pública con sus servicios y profesionales
        String negocio = mvc.perform(get("/api/publico/" + DatosDemo.SLUG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicios", hasSize(4)))
                .andExpect(jsonPath("$.recursos", hasSize(2)))
                .andReturn().getResponse().getContentAsString();

        // La propietaria y la empleada pueden entrar
        login(DatosDemo.EMAIL_PROPIETARIA);
        String tokenEmpleada = login(DatosDemo.EMAIL_EMPLEADA);

        // La empleada ve su agenda con las reservas de ejemplo de los próximos días
        LocalDate hoy = LocalDate.now(ZoneId.of("Europe/Madrid"));
        mvc.perform(get("/api/panel/reservas")
                        .header("Authorization", "Bearer " + tokenEmpleada)
                        .param("desde", hoy.atStartOfDay(ZoneId.of("Europe/Madrid")).toInstant().toString())
                        .param("hasta", hoy.plusDays(10).atStartOfDay(ZoneId.of("Europe/Madrid")).toInstant().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(15)));

        // El próximo lunes hay huecos libres aunque haya citas
        LocalDate lunes = hoy.plusDays(1);
        while (lunes.getDayOfWeek() != DayOfWeek.MONDAY) {
            lunes = lunes.plusDays(1);
        }
        String corte = JsonPath.<List<String>>read(negocio, "$.servicios[?(@.nombre == 'Corte de pelo')].id").getFirst();
        mvc.perform(get("/api/publico/" + DatosDemo.SLUG + "/disponibilidad")
                        .param("servicioId", corte).param("fecha", lunes.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.huecos").isNotEmpty());

        // Idempotente: una segunda carga no crea nada
        assertThat(datos.cargar()).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM negocio WHERE slug = ?", Integer.class, DatosDemo.SLUG))
                .isEqualTo(1);
    }

    private String login(String email) throws Exception {
        String respuesta = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, DatosDemo.PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.accessToken");
    }
}
