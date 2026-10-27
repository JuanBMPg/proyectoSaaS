package com.juan.reservas.publico.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Huecos libres de un día. Los instantes van en UTC; el frontend los muestra en
 * {@code zonaHoraria}, la del negocio, y no en la del navegador del visitante.
 */
public record DisponibilidadDto(LocalDate fecha, String zonaHoraria, List<HuecoDto> huecos) {

    public record HuecoDto(UUID recursoId, Instant inicio, Instant fin) {
    }
}
