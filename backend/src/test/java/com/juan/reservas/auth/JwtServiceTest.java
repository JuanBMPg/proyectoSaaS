package com.juan.reservas.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.juan.reservas.negocio.Negocio;

/** Test unitario: emite tokens con un reloj fijo y los valida con el decodificador real. */
class JwtServiceTest {

    private static final Instant AHORA = Instant.parse("2026-06-02T08:00:00Z");
    private static final AuthProperties PROPS = new AuthProperties(
            "secreto-de-prueba-con-mas-de-32-caracteres", "reservas",
            Duration.ofMinutes(15), Duration.ofDays(7), true);

    private final JwtConfig config = new JwtConfig();
    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
    private final JwtService service = new JwtService(
            config.jwtEncoder(config.jwtSecretKey(PROPS)), PROPS, reloj);

    private final Negocio negocio = new Negocio("Peluquería Lola", "lola", AHORA);
    private final Usuario usuario = new Usuario(negocio, "lola@example.com", "hash", "Lola", Rol.PROPIETARIO, AHORA);

    @Test
    void elTokenLlevaUsuarioNegocioYRol() {
        TokenEmitido token = service.emitirAccessToken(usuario);

        Jwt jwt = decodificadorConReloj(reloj).decode(token.valor());

        assertThat(jwt.getSubject()).isEqualTo(usuario.getId().toString());
        assertThat(jwt.getClaimAsString(JwtService.CLAIM_NEGOCIO_ID)).isEqualTo(negocio.getId().toString());
        assertThat(jwt.getClaimAsString(JwtService.CLAIM_ROL)).isEqualTo("PROPIETARIO");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("reservas");
    }

    @Test
    void caducaALos15Minutos() {
        TokenEmitido token = service.emitirAccessToken(usuario);

        assertThat(token.expiraEn()).isEqualTo(AHORA.plus(Duration.ofMinutes(15)));
    }

    @Test
    void seRechazaCuandoHaCaducado() {
        TokenEmitido token = service.emitirAccessToken(usuario);
        // 15 min de vida + 60 s de margen por defecto para desajustes de reloj
        Clock dentroDe17Minutos = Clock.offset(reloj, Duration.ofMinutes(17));

        assertThatThrownBy(() -> decodificadorConReloj(dentroDe17Minutos).decode(token.valor()))
                .isInstanceOf(BadJwtException.class);
    }

    @Test
    void seRechazaSiEstaFirmadoConOtraClave() {
        AuthProperties otraClave = new AuthProperties(
                "otro-secreto-distinto-con-mas-de-32-caracteres", "reservas",
                Duration.ofMinutes(15), Duration.ofDays(7), true);
        JwtService falsificador = new JwtService(
                config.jwtEncoder(config.jwtSecretKey(otraClave)), otraClave, reloj);

        String tokenFalso = falsificador.emitirAccessToken(usuario).valor();

        assertThatThrownBy(() -> decodificadorConReloj(reloj).decode(tokenFalso))
                .isInstanceOf(BadJwtException.class);
    }

    @Test
    void usuarioActualSeConstruyeDesdeElToken() {
        Jwt jwt = decodificadorConReloj(reloj).decode(service.emitirAccessToken(usuario).valor());

        UsuarioActual actual = UsuarioActual.desde(jwt);

        assertThat(actual.usuarioId()).isEqualTo(usuario.getId());
        assertThat(actual.negocioId()).isEqualTo(negocio.getId());
        assertThat(actual.esPropietario()).isTrue();
    }

    private JwtDecoder decodificadorConReloj(Clock clock) {
        return config.jwtDecoder(config.jwtSecretKey(PROPS), PROPS, clock);
    }
}
