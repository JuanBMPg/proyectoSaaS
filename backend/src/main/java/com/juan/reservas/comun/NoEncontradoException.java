package com.juan.reservas.comun;

/**
 * El elemento no existe o pertenece a otro negocio. En ambos casos se responde 404,
 * para no revelar a un usuario la existencia de datos de otros negocios.
 */
public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
