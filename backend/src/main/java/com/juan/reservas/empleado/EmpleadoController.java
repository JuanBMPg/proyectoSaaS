package com.juan.reservas.empleado;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.juan.reservas.auth.UsuarioActual;
import com.juan.reservas.config.OpenApiConfig;
import com.juan.reservas.empleado.dto.EmpleadoDto;
import com.juan.reservas.empleado.dto.EmpleadoRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/panel/empleados")
@PreAuthorize("hasRole('PROPIETARIO')")
@Tag(name = "Panel: empleados")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
public class EmpleadoController {

    private final EmpleadoService service;

    public EmpleadoController(EmpleadoService service) {
        this.service = service;
    }

    @Operation(summary = "Empleados del negocio con su recurso")
    @GetMapping
    public List<EmpleadoDto> listar(UsuarioActual usuario) {
        return service.listar(usuario.negocioId());
    }

    @Operation(summary = "Da de alta un empleado vinculado a un recurso, con una contraseña inicial")
    @PostMapping
    public ResponseEntity<EmpleadoDto> crear(UsuarioActual usuario, @Valid @RequestBody EmpleadoRequest peticion) {
        EmpleadoDto creado = service.crear(usuario.negocioId(), peticion);
        return ResponseEntity.created(URI.create("/api/panel/empleados/" + creado.id())).body(creado);
    }
}
