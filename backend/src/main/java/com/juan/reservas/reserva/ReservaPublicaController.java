package com.juan.reservas.reserva;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.juan.reservas.reserva.dto.ReservaCreadaDto;
import com.juan.reservas.reserva.dto.ReservaPublicaDto;
import com.juan.reservas.reserva.dto.ReservaPublicaRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Reservas desde la web pública: sin login. "reservas" es un slug reservado, así que no choca con /{slug}. */
@RestController
@Tag(name = "Web pública")
public class ReservaPublicaController {

    private final ReservaService service;

    public ReservaPublicaController(ReservaService service) {
        this.service = service;
    }

    @Operation(summary = "Crea una reserva en un hueco libre (409 si se acaba de ocupar, 422 si incumple la antelación)")
    @PostMapping("/api/publico/{slug}/reservas")
    public ResponseEntity<ReservaCreadaDto> crear(@PathVariable String slug,
            @Valid @RequestBody ReservaPublicaRequest peticion) {
        ReservaCreadaDto creada = service.crearDesdeWeb(slug, peticion);
        return ResponseEntity.created(URI.create("/api/publico/reservas/" + creada.token())).body(creada);
    }

    @Operation(summary = "Una reserva vista desde su enlace secreto")
    @GetMapping("/api/publico/reservas/{token}")
    public ReservaPublicaDto ver(@PathVariable String token) {
        return service.buscarPorToken(token);
    }
}
