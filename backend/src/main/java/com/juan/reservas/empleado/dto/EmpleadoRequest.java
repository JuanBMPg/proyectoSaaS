package com.juan.reservas.empleado.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de un empleado por el propietario.
 *
 * @param password  contraseña inicial que el propietario le comunica al empleado
 * @param recursoId recurso (normalmente una persona) cuya agenda verá y gestionará
 */
public record EmpleadoRequest(
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull UUID recursoId) {
}
