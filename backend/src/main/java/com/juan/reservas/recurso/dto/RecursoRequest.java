package com.juan.reservas.recurso.dto;

import com.juan.reservas.recurso.TipoRecurso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta o edición de un recurso.
 *
 * @param activo opcional: al crear, el recurso nace activo; al editar, {@code null} lo deja
 *               como está y {@code true} lo reactiva
 */
public record RecursoRequest(
        @NotBlank @Size(max = 120) String nombre,
        @NotNull TipoRecurso tipo,
        Boolean activo) {
}
