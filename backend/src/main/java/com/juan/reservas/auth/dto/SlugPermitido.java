package com.juan.reservas.auth.dto;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * El slug no puede coincidir con una ruta de la propia aplicación. La página pública de un
 * negocio vive en /{slug}: un negocio llamado "panel" o "login" quedaría inaccesible.
 */
@Documented
@Constraint(validatedBy = SlugPermitido.Validador.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SlugPermitido {

    String message() default "esa dirección está reservada por la aplicación";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Rutas del frontend y del backend, y nombres que podrían usarse en el futuro. */
    Set<String> RESERVADOS = Set.of(
            "login", "registro", "panel", "api", "admin", "publico", "reservas", "reserva",
            "assets", "swagger-ui", "v3", "error", "ayuda", "precios", "legal", "privacidad");

    class Validador implements ConstraintValidator<SlugPermitido, String> {

        @Override
        public boolean isValid(String slug, ConstraintValidatorContext contexto) {
            // Vacío o nulo ya lo señala @NotBlank
            return slug == null || !RESERVADOS.contains(slug);
        }
    }
}
