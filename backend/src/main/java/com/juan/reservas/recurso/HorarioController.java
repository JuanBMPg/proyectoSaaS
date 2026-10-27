package com.juan.reservas.recurso;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.juan.reservas.auth.UsuarioActual;
import com.juan.reservas.config.OpenApiConfig;
import com.juan.reservas.recurso.dto.HorarioDto;
import com.juan.reservas.recurso.dto.HorarioRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/panel/recursos/{recursoId}/horario")
@PreAuthorize("hasRole('PROPIETARIO')")
@Tag(name = "Panel: recursos")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
public class HorarioController {

    private final HorarioService service;

    public HorarioController(HorarioService service) {
        this.service = service;
    }

    @Operation(summary = "Horario semanal del recurso (horas locales del negocio)")
    @GetMapping
    public HorarioDto obtener(UsuarioActual usuario, @PathVariable UUID recursoId) {
        return service.obtener(usuario.negocioId(), recursoId);
    }

    @Operation(summary = "Sustituye el horario semanal completo del recurso")
    @PutMapping
    public HorarioDto reemplazar(UsuarioActual usuario, @PathVariable UUID recursoId,
            @Valid @RequestBody HorarioRequest peticion) {
        return service.reemplazar(usuario.negocioId(), recursoId, peticion);
    }
}
