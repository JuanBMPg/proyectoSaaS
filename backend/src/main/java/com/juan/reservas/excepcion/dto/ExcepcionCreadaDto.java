package com.juan.reservas.excepcion.dto;

/**
 * Respuesta al crear una excepción.
 *
 * @param reservasActivas reservas pendientes o confirmadas de los recursos afectados en esos
 *                        días. Crear la excepción no las cancela (puede que el negocio quiera
 *                        atenderlas igualmente), así que el panel avisa para que las revise.
 */
public record ExcepcionCreadaDto(ExcepcionDto excepcion, long reservasActivas) {
}
