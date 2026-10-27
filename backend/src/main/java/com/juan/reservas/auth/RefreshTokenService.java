package com.juan.reservas.auth;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.comun.NoAutorizadoException;
import com.juan.reservas.comun.TokenSecreto;

/**
 * Gestiona los refresh tokens: valores aleatorios opacos (no JWT) guardados como hash.
 * <p>
 * Cada uso rota el token: el usado se revoca y se emite uno nuevo. Así un token robado
 * deja de servir en cuanto el usuario legítimo refresca su sesión.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final AuthProperties props;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenRepository repository, AuthProperties props, Clock clock) {
        this.repository = repository;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public TokenEmitido emitir(Usuario usuario) {
        String valor = TokenSecreto.generar();
        Instant expiraEn = Instant.now(clock).plus(props.refreshTokenTtl());
        repository.save(new RefreshToken(usuario, TokenSecreto.hash(valor), expiraEn));
        return new TokenEmitido(valor, expiraEn);
    }

    /**
     * Revoca el token recibido y devuelve su usuario, para que quien llama emita los nuevos.
     *
     * @throws NoAutorizadoException si el token no existe, está revocado o ha caducado
     */
    @Transactional
    public Usuario consumir(String valor) {
        RefreshToken token = repository.findByTokenHash(TokenSecreto.hash(valor))
                .filter(t -> t.esValido(Instant.now(clock)))
                .orElseThrow(() -> new NoAutorizadoException("La sesión ha caducado; vuelve a iniciar sesión"));
        token.revocar();
        return token.getUsuario();
    }

    /** Cierra la sesión. Si el token no existe no hace nada: el logout siempre "funciona". */
    @Transactional
    public void revocar(String valor) {
        repository.findByTokenHash(TokenSecreto.hash(valor)).ifPresent(RefreshToken::revocar);
    }
}
