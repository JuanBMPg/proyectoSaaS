package com.juan.reservas.negocio;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.juan.reservas.auth.UsuarioActual;
import com.juan.reservas.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/panel/negocio")
@Tag(name = "Panel: negocio")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
public class NegocioController {

    private final NegocioService service;

    public NegocioController(NegocioService service) {
        this.service = service;
    }

    /** El negocio sale del token: no hay forma de pedir el de otro. */
    @Operation(summary = "Datos y reglas del negocio del usuario")
    @GetMapping
    @PreAuthorize("hasRole('PROPIETARIO')")
    public NegocioDto obtener(UsuarioActual usuario) {
        return service.obtener(usuario.negocioId());
    }
}
