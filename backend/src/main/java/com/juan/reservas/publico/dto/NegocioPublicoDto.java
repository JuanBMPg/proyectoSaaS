package com.juan.reservas.publico.dto;

import java.util.List;

/**
 * Lo que ve cualquier visitante de la página pública de un negocio. Son DTOs propios de la web
 * pública, distintos de los del panel, para no exponer por accidente datos internos
 * (márgenes, recursos inactivos, reglas de cancelación...).
 *
 * @param etiquetaRecurso cómo llama el negocio a sus recursos: "Profesional", "Pista"...
 */
public record NegocioPublicoDto(
        String nombre,
        String slug,
        String zonaHoraria,
        String direccion,
        String telefono,
        String etiquetaRecurso,
        int antelacionMaximaDias,
        List<ServicioPublicoDto> servicios,
        List<RecursoPublicoDto> recursos) {
}
