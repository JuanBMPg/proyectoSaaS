package com.juan.reservas.reserva;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.juan.reservas.auth.UsuarioActual;
import com.juan.reservas.config.OpenApiConfig;
import com.juan.reservas.reserva.dto.CambioEstadoRequest;
import com.juan.reservas.reserva.dto.ReservaManualRequest;
import com.juan.reservas.reserva.dto.ReservaPanelDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Agenda del panel: propietario y empleados (cada empleado, solo su recurso). */
@RestController
@RequestMapping("/api/panel/reservas")
@PreAuthorize("hasAnyRole('PROPIETARIO', 'EMPLEADO')")
@Tag(name = "Panel: agenda")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT)
public class ReservaPanelController {

    private final ReservaPanelService service;

    public ReservaPanelController(ReservaPanelService service) {
        this.service = service;
    }

    @Operation(summary = "Reservas que se cruzan con [desde, hasta) (máximo 62 días)")
    @GetMapping
    public List<ReservaPanelDto> agenda(UsuarioActual usuario,
            @Parameter(description = "Instante ISO-8601, p. ej. 2026-11-09T00:00:00Z") @RequestParam Instant desde,
            @RequestParam Instant hasta,
            @Parameter(description = "Solo para el propietario: filtrar por un recurso")
            @RequestParam(required = false) UUID recursoId) {
        return service.agenda(usuario, desde, hasta, recursoId);
    }

    @Operation(summary = "Solicitudes pendientes de confirmar que aún no han empezado")
    @GetMapping("/pendientes")
    public List<ReservaPanelDto> pendientes(UsuarioActual usuario) {
        return service.pendientes(usuario);
    }

    @Operation(summary = "Reserva manual (p. ej. por teléfono): confirmada, sin reglas de antelación ni horario")
    @PostMapping
    public ResponseEntity<ReservaPanelDto> crear(UsuarioActual usuario,
            @Valid @RequestBody ReservaManualRequest peticion) {
        ReservaPanelDto creada = service.crearManual(usuario, peticion);
        return ResponseEntity.created(URI.create("/api/panel/reservas/" + creada.id())).body(creada);
    }

    @Operation(summary = "Confirmar, cancelar, completar o marcar como no presentado")
    @PatchMapping("/{id}/estado")
    public ReservaPanelDto cambiarEstado(UsuarioActual usuario, @PathVariable UUID id,
            @Valid @RequestBody CambioEstadoRequest peticion) {
        return service.cambiarEstado(usuario, id, peticion.estado());
    }
}
