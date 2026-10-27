package com.juan.reservas.recurso;

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
import com.juan.reservas.recurso.dto.AsignacionServiciosRequest;
import com.juan.reservas.recurso.dto.RecursoDto;
import com.juan.reservas.recurso.dto.RecursoRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/panel/recursos")
@PreAuthorize("hasRole('PROPIETARIO')")
@Tag(name = "Panel: recursos")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
public class RecursoController {

    private final RecursoService service;

    public RecursoController(RecursoService service) {
        this.service = service;
    }

    @Operation(summary = "Lista los recursos del negocio, activos e inactivos, con sus servicios")
    @GetMapping
    public List<RecursoDto> listar(UsuarioActual usuario) {
        return service.listar(usuario.negocioId());
    }

    @Operation(summary = "Crea un recurso")
    @PostMapping
    public ResponseEntity<RecursoDto> crear(UsuarioActual usuario, @Valid @RequestBody RecursoRequest peticion) {
        RecursoDto creado = service.crear(usuario.negocioId(), peticion);
        return ResponseEntity.created(URI.create("/api/panel/recursos/" + creado.id())).body(creado);
    }

    @Operation(summary = "Edita un recurso (activo = true lo reactiva)")
    @PutMapping("/{id}")
    public RecursoDto actualizar(UsuarioActual usuario, @PathVariable UUID id,
            @Valid @RequestBody RecursoRequest peticion) {
        return service.actualizar(usuario.negocioId(), id, peticion);
    }

    @Operation(summary = "Desactiva un recurso (no se borra: lo usan reservas pasadas)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(UsuarioActual usuario, @PathVariable UUID id) {
        service.desactivar(usuario.negocioId(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Sustituye el conjunto de servicios que presta el recurso")
    @PutMapping("/{id}/servicios")
    public RecursoDto asignarServicios(UsuarioActual usuario, @PathVariable UUID id,
            @Valid @RequestBody AsignacionServiciosRequest peticion) {
        return service.asignarServicios(usuario.negocioId(), id, peticion);
    }
}
