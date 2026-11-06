package com.juan.reservas.excepcion;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.juan.reservas.auth.UsuarioActual;
import com.juan.reservas.config.OpenApiConfig;
import com.juan.reservas.excepcion.dto.ExcepcionCreadaDto;
import com.juan.reservas.excepcion.dto.ExcepcionDto;
import com.juan.reservas.excepcion.dto.ExcepcionRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/panel/excepciones")
@PreAuthorize("hasRole('PROPIETARIO')")
@Tag(name = "Panel: excepciones del horario")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
public class ExcepcionController {

    private final ExcepcionService service;

    public ExcepcionController(ExcepcionService service) {
        this.service = service;
    }

    @Operation(summary = "Vacaciones, festivos y horarios especiales vigentes o futuros")
    @GetMapping
    public List<ExcepcionDto> listar(UsuarioActual usuario,
            @Parameter(description = "Desde qué día (por defecto, hoy en la zona del negocio)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde) {
        return service.listar(usuario.negocioId(), desde);
    }

    @Operation(summary = "Crea una excepción: sin horas cierra esos días; con horas los sustituye por esa franja")
    @PostMapping
    public ResponseEntity<ExcepcionCreadaDto> crear(UsuarioActual usuario,
            @Valid @RequestBody ExcepcionRequest peticion) {
        ExcepcionCreadaDto creada = service.crear(usuario.negocioId(), peticion);
        return ResponseEntity.created(URI.create("/api/panel/excepciones/" + creada.excepcion().id())).body(creada);
    }

    @Operation(summary = "Borra una excepción (vuelve a regir el horario semanal)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(UsuarioActual usuario, @PathVariable UUID id) {
        service.borrar(usuario.negocioId(), id);
        return ResponseEntity.noContent().build();
    }
}
