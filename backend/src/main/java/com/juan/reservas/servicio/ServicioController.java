package com.juan.reservas.servicio;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.juan.reservas.auth.UsuarioActual;
import com.juan.reservas.config.OpenApiConfig;
import com.juan.reservas.servicio.dto.ServicioDto;
import com.juan.reservas.servicio.dto.ServicioRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/panel/servicios")
@PreAuthorize("hasRole('PROPIETARIO')")
@Tag(name = "Panel: servicios")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
public class ServicioController {

    private final ServicioService service;

    public ServicioController(ServicioService service) {
        this.service = service;
    }

    @Operation(summary = "Lista los servicios del negocio, activos e inactivos")
    @GetMapping
    public List<ServicioDto> listar(UsuarioActual usuario) {
        return service.listar(usuario.negocioId());
    }

    @Operation(summary = "Crea un servicio")
    @PostMapping
    public ResponseEntity<ServicioDto> crear(UsuarioActual usuario, @Valid @RequestBody ServicioRequest peticion) {
        ServicioDto creado = service.crear(usuario.negocioId(), peticion);
        return ResponseEntity.created(URI.create("/api/panel/servicios/" + creado.id())).body(creado);
    }

    @Operation(summary = "Edita un servicio (activo = true lo reactiva)")
    @PutMapping("/{id}")
    public ServicioDto actualizar(UsuarioActual usuario, @PathVariable UUID id,
            @Valid @RequestBody ServicioRequest peticion) {
        return service.actualizar(usuario.negocioId(), id, peticion);
    }

    @Operation(summary = "Desactiva un servicio (no se borra: lo usan reservas pasadas)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(UsuarioActual usuario, @PathVariable UUID id) {
        service.desactivar(usuario.negocioId(), id);
        return ResponseEntity.noContent().build();
    }
}
