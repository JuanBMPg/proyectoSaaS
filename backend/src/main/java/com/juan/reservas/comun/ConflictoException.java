package com.juan.reservas.comun;

/** La operación choca con el estado actual de los datos (409), p. ej. un hueco ya ocupado. */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
