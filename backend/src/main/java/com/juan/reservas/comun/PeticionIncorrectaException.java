package com.juan.reservas.comun;

/** La petición no tiene sentido aunque cada campo sea válido (400), p. ej. un rango de fechas invertido. */
public class PeticionIncorrectaException extends RuntimeException {

    public PeticionIncorrectaException(String mensaje) {
        super(mensaje);
    }
}
