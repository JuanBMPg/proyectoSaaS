package com.juan.reservas.auth.dto;

import java.time.Instant;

/**
 * Respuesta de registro, login y refresh. El refresh token NO va aquí: viaja en una
 * cookie httpOnly que JavaScript no puede leer.
 */
public record AccessTokenResponse(String accessToken, String tokenType, Instant expiraEn) {

    public static AccessTokenResponse bearer(String accessToken, Instant expiraEn) {
        return new AccessTokenResponse(accessToken, "Bearer", expiraEn);
    }
}
