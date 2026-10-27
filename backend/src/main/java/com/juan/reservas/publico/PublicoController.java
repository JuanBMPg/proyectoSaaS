package com.juan.reservas.publico;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.juan.reservas.publico.dto.DisponibilidadDto;
import com.juan.reservas.publico.dto.NegocioPublicoDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Web pública: sin login, el negocio se identifica por el slug de su URL. */
@RestController
@RequestMapping("/api/publico/{slug}")
@Tag(name = "Web pública")
public class PublicoController {

    private final PublicoService service;

    public PublicoController(PublicoService service) {
        this.service = service;
    }

    @Operation(summary = "Datos públicos del negocio, con sus servicios y recursos reservables")
    @GetMapping
    public NegocioPublicoDto negocio(@PathVariable String slug) {
        return service.negocio(slug);
    }

    @Operation(summary = "Huecos libres de un día para un servicio (instantes en UTC)")
    @GetMapping("/disponibilidad")
    public DisponibilidadDto disponibilidad(
            @PathVariable String slug,
            @RequestParam UUID servicioId,
            @Parameter(description = "Día en la zona horaria del negocio, p. ej. 2026-06-02")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @Parameter(description = "Opcional: solo los huecos de este recurso")
            @RequestParam(required = false) UUID recursoId) {
        return service.disponibilidad(slug, servicioId, fecha, recursoId);
    }
}
