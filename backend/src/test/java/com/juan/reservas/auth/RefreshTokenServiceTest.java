package com.juan.reservas.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.juan.reservas.comun.NoAutorizadoException;
import com.juan.reservas.negocio.Negocio;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant AHORA = Instant.parse("2026-06-02T08:00:00Z");
    private static final AuthProperties PROPS = new AuthProperties(
            "secreto-de-prueba-con-mas-de-32-caracteres", "reservas",
            Duration.ofMinutes(15), Duration.ofDays(7), true);

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, PROPS, Clock.fixed(AHORA, ZoneOffset.UTC));
        Negocio negocio = new Negocio("Peluquería Lola", "lola", AHORA);
        usuario = new Usuario(negocio, "lola@example.com", "hash", "Lola", Rol.PROPIETARIO, AHORA);
    }

    @Test
    void emiteUnTokenAleatorioYGuardaSoloSuHash() {
        TokenEmitido token = service.emitir(usuario);

        ArgumentCaptor<RefreshToken> guardado = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(guardado.capture());
        assertThat(token.valor()).hasSizeGreaterThanOrEqualTo(43); // 32 bytes en Base64 URL
        assertThat(token.expiraEn()).isEqualTo(AHORA.plus(Duration.ofDays(7)));
        assertThat(guardado.getValue().getExpiraEn()).isEqualTo(token.expiraEn());
    }

    @Test
    void dosTokensEmitidosSonDistintos() {
        assertThat(service.emitir(usuario).valor()).isNotEqualTo(service.emitir(usuario).valor());
    }

    @Test
    void consumirUnTokenValidoLoRevocaYDevuelveSuUsuario() {
        RefreshToken token = new RefreshToken(usuario, "hash", AHORA.plus(Duration.ofDays(1)));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

        Usuario resultado = service.consumir("valor");

        assertThat(resultado).isSameAs(usuario);
        assertThat(token.isRevocado()).isTrue();
    }

    @Test
    void noSePuedeConsumirUnTokenRevocado() {
        RefreshToken token = new RefreshToken(usuario, "hash", AHORA.plus(Duration.ofDays(1)));
        token.revocar();
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.consumir("valor")).isInstanceOf(NoAutorizadoException.class);
    }

    @Test
    void noSePuedeConsumirUnTokenCaducado() {
        RefreshToken token = new RefreshToken(usuario, "hash", AHORA.minusSeconds(1));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.consumir("valor")).isInstanceOf(NoAutorizadoException.class);
    }

    @Test
    void noSePuedeConsumirUnTokenInexistente() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consumir("valor")).isInstanceOf(NoAutorizadoException.class);
    }
}
