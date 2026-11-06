package com.juan.reservas.disponibilidad;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Lo que la calculadora necesita saber de un recurso para un día concreto.
 *
 * @param franjas  su horario de ESE día en hora local, ya con las excepciones aplicadas
 * @param ocupados intervalos que ya no se pueden usar: reservas pendientes o confirmadas
 *                 como [inicio, fin_bloqueo)
 */
public record AgendaRecurso(UUID recursoId, List<FranjaLocal> franjas, List<Intervalo> ocupados) {

    public AgendaRecurso {
        Objects.requireNonNull(recursoId, "recursoId");
        franjas = List.copyOf(franjas);
        ocupados = List.copyOf(ocupados);
    }
}
