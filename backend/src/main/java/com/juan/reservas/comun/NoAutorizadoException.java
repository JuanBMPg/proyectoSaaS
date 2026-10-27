package com.juan.reservas.comun;

/** Credenciales o sesión no válidas (401), p. ej. contraseña incorrecta o refresh token caducado. */
public class NoAutorizadoException extends RuntimeException {

    public NoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
