package com.juan.reservas.auth;

import java.time.Instant;

/** Un token recién emitido (access o refresh) y su fecha de caducidad. */
public record TokenEmitido(String valor, Instant expiraEn) {
}
