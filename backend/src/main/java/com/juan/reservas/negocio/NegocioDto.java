package com.juan.reservas.negocio;

import java.util.UUID;

/** Datos y reglas del negocio que ve el propietario en su panel. */
public record NegocioDto(
        UUID id,
        String nombre,
        String slug,
        String zonaHoraria,
        String email,
        String telefono,
        String direccion,
        String etiquetaRecurso,
        int antelacionMinimaMin,
        int antelacionMaximaDias,
        int plazoCancelacionHoras,
        boolean confirmacionAutomatica) {
}
