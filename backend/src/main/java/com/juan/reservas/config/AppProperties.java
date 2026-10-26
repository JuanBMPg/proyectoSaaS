package com.juan.reservas.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Configuración propia de la aplicación (prefijo {@code app} en application.yml).
 * Al ser un record validado, la aplicación no arranca si falta algún valor obligatorio.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(@Valid @NotNull Cors cors, @Valid @NotNull Mail mail) {

    public record Cors(@NotEmpty List<String> allowedOrigins) {
    }

    public record Mail(@NotBlank String from) {
    }
}
