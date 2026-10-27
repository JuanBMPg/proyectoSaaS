package com.juan.reservas.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.comun.NoAutorizadoException;

/**
 * Gestiona los refresh tokens: valores aleatorios opacos (no JWT) guardados como hash.
 * <p>
 * Cada uso rota el token: el usado se revoca y se emite uno nuevo. Así un token robado
 * deja de servir en cuanto el usuario legítimo refresca su sesión.
 */
@Service
public class RefreshTokenService {

    private static final int BYTES_TOKEN = 32; // 256 bits de entropía

    private final RefreshTokenRepository repository;
    private final AuthProperties props;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository, AuthProperties props, Clock clock) {
        this.repository = repository;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public TokenEmitido emitir(Usuario usuario) {
        byte[] bytes = new byte[BYTES_TOKEN];
        random.nextBytes(bytes);
        String valor = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Instant expiraEn = Instant.now(clock).plus(props.refreshTokenTtl());
        repository.save(new RefreshToken(usuario, hash(valor), expiraEn));
        return new TokenEmitido(valor, expiraEn);
    }

    /**
     * Revoca el token recibido y devuelve su usuario, para que quien llama emita los nuevos.
     *
     * @throws NoAutorizadoException si el token no existe, está revocado o ha caducado
     */
    @Transactional
    public Usuario consumir(String valor) {
        RefreshToken token = repository.findByTokenHash(hash(valor))
                .filter(t -> t.esValido(Instant.now(clock)))
                .orElseThrow(() -> new NoAutorizadoException("La sesión ha caducado; vuelve a iniciar sesión"));
        token.revocar();
        return token.getUsuario();
    }

    /** Cierra la sesión. Si el token no existe no hace nada: el logout siempre "funciona". */
    @Transactional
    public void revocar(String valor) {
        repository.findByTokenHash(hash(valor)).ifPresent(RefreshToken::revocar);
    }

    /**
     * SHA-256 basta (no hace falta BCrypt): el token ya tiene 256 bits aleatorios,
     * así que no se puede adivinar por fuerza bruta como una contraseña.
     */
    static String hash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM", e);
        }
    }
}
