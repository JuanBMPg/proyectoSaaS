package com.juan.reservas.comun;

/**
 * La petición es válida en forma pero incumple una regla del negocio (422),
 * p. ej. reservar sin la antelación mínima o cancelar fuera de plazo.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
