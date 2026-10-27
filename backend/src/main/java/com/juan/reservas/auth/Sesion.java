package com.juan.reservas.auth;

/** Par de tokens que se entrega al iniciar o renovar una sesión. */
public record Sesion(TokenEmitido accessToken, TokenEmitido refreshToken) {
}
