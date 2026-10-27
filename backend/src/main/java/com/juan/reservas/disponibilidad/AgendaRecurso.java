package com.juan.reservas.disponibilidad;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Lo que la calculadora necesita saber de un recurso para un día concreto.
 *
 * @param franjas  su horario de ESE día de la semana, en hora local
 * @param ocupados intervalos que ya no se pueden usar: reservas pendientes o confirmadas
 *                 como [inicio, fin_bloqueo) y, en la fase 2, las excepciones del horario
 */
public record AgendaRecurso(UUID recursoId, List<FranjaLocal> franjas, List<Intervalo> ocupados) {

    public AgendaRecurso {
        Objects.requireNonNull(recursoId, "recursoId");
        franjas = List.copyOf(franjas);
        ocupados = List.copyOf(ocupados);
    }
}
