package com.juan.reservas.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.juan.reservas.auth.dto.LoginRequest;
import com.juan.reservas.auth.dto.RegistroRequest;
import com.juan.reservas.comun.ConflictoException;
import com.juan.reservas.comun.NoAutorizadoException;
import com.juan.reservas.negocio.Negocio;
import com.juan.reservas.negocio.NegocioRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant AHORA = Instant.parse("2026-06-02T08:00:00Z");

    @Mock
    private NegocioRepository negocioRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService service;
    private Usuario lola;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn("hash-ficticio");
        service = new AuthService(negocioRepository, usuarioRepository, passwordEncoder, jwtService,
                refreshTokenService, Clock.fixed(AHORA, ZoneOffset.UTC));
        lola = new Usuario(new Negocio("Peluquería Lola", "lola", AHORA),
                "lola@example.com", "hash-real", "Lola", Rol.PROPIETARIO, AHORA);
    }

    @Test
    void loginCorrectoIniciaSesion() {
        when(usuarioRepository.findByEmail("lola@example.com")).thenReturn(Optional.of(lola));
        when(passwordEncoder.matches("secreta123", "hash-real")).thenReturn(true);
        when(jwtService.emitirAccessToken(lola)).thenReturn(new TokenEmitido("access", AHORA));
        when(refreshTokenService.emitir(lola)).thenReturn(new TokenEmitido("refresh", AHORA));

        Sesion sesion = service.login(new LoginRequest("  Lola@Example.COM ", "secreta123"));

        assertThat(sesion.accessToken().valor()).isEqualTo("access");
        assertThat(sesion.refreshToken().valor()).isEqualTo("refresh");
    }

    @Test
    void contraseñaIncorrectaDevuelveElMensajeGenerico() {
        when(usuarioRepository.findByEmail("lola@example.com")).thenReturn(Optional.of(lola));
        when(passwordEncoder.matches(anyString(), eq("hash-real"))).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("lola@example.com", "mala")))
                .isInstanceOf(NoAutorizadoException.class)
                .hasMessage(AuthService.CREDENCIALES_INCORRECTAS);
    }

    @Test
    void emailInexistenteDevuelveElMismoMensajeYComparaIgualmenteUnaContraseña() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("nadie@example.com", "loquesea")))
                .isInstanceOf(NoAutorizadoException.class)
                .hasMessage(AuthService.CREDENCIALES_INCORRECTAS);
        // Se compara contra el hash ficticio para que tarde lo mismo que con un email real
        verify(passwordEncoder).matches("loquesea", "hash-ficticio");
    }

    @Test
    void usuarioDesactivadoNoPuedeEntrarAunqueLaContraseñaSeaCorrecta() {
        lola.desactivar();
        when(usuarioRepository.findByEmail("lola@example.com")).thenReturn(Optional.of(lola));
        when(passwordEncoder.matches("secreta123", "hash-real")).thenReturn(true);

        assertThatThrownBy(() -> service.login(new LoginRequest("lola@example.com", "secreta123")))
                .isInstanceOf(NoAutorizadoException.class);
    }

    @Test
    void registroConSlugRepetidoDevuelveConflictoYNoGuardaNada() {
        when(negocioRepository.existsBySlug("lola")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(registro("lola", "nueva@example.com")))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("slug");
        verify(negocioRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registroConEmailRepetidoDevuelveConflicto() {
        when(usuarioRepository.existsByEmail("lola@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(registro("otro-slug", "LOLA@example.com")))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("email");
    }

    @Test
    void refrescarConUsuarioDesactivadoDevuelve401() {
        lola.desactivar();
        when(refreshTokenService.consumir("refresh")).thenReturn(lola);

        assertThatThrownBy(() -> service.refrescar("refresh")).isInstanceOf(NoAutorizadoException.class);
        verify(jwtService, never()).emitirAccessToken(any());
    }

    private static RegistroRequest registro(String slug, String email) {
        return new RegistroRequest("Peluquería Lola", slug, "Lola", email, "secreta123");
    }
}
