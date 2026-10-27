package com.juan.reservas.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Configuración de autenticación (prefijo {@code app.auth}).
 *
 * @param jwtSecret          clave HMAC para firmar los JWT. HS256 exige al menos 256 bits (32 bytes);
 *                           si es más corta la aplicación no arranca.
 * @param jwtIssuer          emisor que se escribe y se comprueba en cada token
 * @param accessTokenTtl     vida del access token (15 min)
 * @param refreshTokenTtl    vida del refresh token (7 días)
 * @param refreshCookieSecure si la cookie del refresh token lleva el atributo Secure (solo HTTPS)
 */
@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        @NotBlank @Size(min = 32, message = "debe tener al menos 32 caracteres (256 bits)") String jwtSecret,
        @NotBlank String jwtIssuer,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration refreshTokenTtl,
        boolean refreshCookieSecure) {
}
