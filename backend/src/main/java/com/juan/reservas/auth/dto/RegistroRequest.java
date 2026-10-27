package com.juan.reservas.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta de un negocio junto con su usuario propietario.
 *
 * @param slug     identificador del negocio en su URL pública (/peluqueria-lola):
 *                 minúsculas, números y guiones, sin guiones al principio ni al final
 * @param password máximo 72 caracteres porque BCrypt ignora lo que pase de 72 bytes
 */
public record RegistroRequest(
        @NotBlank @Size(max = 120) String nombreNegocio,
        @NotBlank @Size(min = 3, max = 80)
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                message = "solo minúsculas, números y guiones (sin guiones al principio ni al final)")
        @SlugPermitido
        String slug,
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(min = 8, max = 72) String password) {
}
