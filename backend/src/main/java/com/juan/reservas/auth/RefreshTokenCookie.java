package com.juan.reservas.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Construye la cookie que transporta el refresh token.
 * <ul>
 *   <li>HttpOnly: JavaScript no puede leerla, así que un XSS no puede robar la sesión larga.</li>
 *   <li>SameSite=Strict: el navegador no la envía en peticiones iniciadas desde otra web (protege de CSRF).</li>
 *   <li>Path=/api/auth: solo viaja a las rutas de autenticación, no en cada petición a la API.</li>
 *   <li>Secure: solo por HTTPS (configurable para desarrollo).</li>
 * </ul>
 */
@Component
public class RefreshTokenCookie {

    public static final String NOMBRE = "refresh_token";
    private static final String RUTA = "/api/auth";

    private final AuthProperties props;
    private final Clock clock;

    public RefreshTokenCookie(AuthProperties props, Clock clock) {
        this.props = props;
        this.clock = clock;
    }

    public ResponseCookie crear(TokenEmitido refreshToken) {
        Duration vida = Duration.between(Instant.now(clock), refreshToken.expiraEn());
        return base(refreshToken.valor()).maxAge(vida).build();
    }

    /** Cookie vacía con Max-Age=0: el navegador borra la que tuviera. */
    public ResponseCookie borrar() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(NOMBRE, valor)
                .httpOnly(true)
                .secure(props.refreshCookieSecure())
                .sameSite("Strict")
                .path(RUTA);
    }
}
