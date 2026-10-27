package com.juan.reservas.auth;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Quién hace la petición, según su JWT. Los controladores del panel lo reciben como parámetro
 * y filtran siempre por {@link #negocioId()}, nunca por un negocioId que llegue en la petición.
 */
public record UsuarioActual(UUID usuarioId, UUID negocioId, Rol rol) {

    public static UsuarioActual desde(Jwt jwt) {
        return new UsuarioActual(
                UUID.fromString(jwt.getSubject()),
                UUID.fromString(jwt.getClaimAsString(JwtService.CLAIM_NEGOCIO_ID)),
                Rol.valueOf(jwt.getClaimAsString(JwtService.CLAIM_ROL)));
    }

    public boolean esPropietario() {
        return rol == Rol.PROPIETARIO;
    }
}
