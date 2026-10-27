package com.juan.reservas.recurso.dto;

import java.util.List;

/** Horario semanal de un recurso, ordenado por día y hora de inicio. */
public record HorarioDto(List<FranjaHorariaDto> franjas) {
}
