package com.juan.reservas.negocio;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.juan.reservas.auth.UsuarioActual;
import com.juan.reservas.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/panel/negocio")
@Tag(name = "Panel: negocio")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
public class NegocioController {

    private final NegocioService service;

    public NegocioController(NegocioService service) {
        this.service = service;
    }

    /**
     * El negocio sale del token: no hay forma de pedir el de otro. Lo pueden leer también los
     * empleados: la agenda necesita su zona horaria y la reserva manual su slug.
     */
    @Operation(summary = "Datos y reglas del negocio del usuario")
    @GetMapping
    @PreAuthorize("hasAnyRole('PROPIETARIO', 'EMPLEADO')")
    public NegocioDto obtener(UsuarioActual usuario) {
        return service.obtener(usuario.negocioId());
    }

    @Operation(summary = "Cambia los datos y las reglas de reserva del negocio (solo el propietario)")
    @PutMapping
    @PreAuthorize("hasRole('PROPIETARIO')")
    public NegocioDto actualizar(UsuarioActual usuario, @Valid @RequestBody NegocioRequest peticion) {
        return service.actualizar(usuario.negocioId(), peticion);
    }
}
