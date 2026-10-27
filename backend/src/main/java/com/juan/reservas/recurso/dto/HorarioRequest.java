package com.juan.reservas.recurso.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Semana COMPLETA de un recurso; sustituye a la anterior.
 * Una lista vacía deja el recurso sin horario (no atiende ningún día).
 */
public record HorarioRequest(
        @NotNull @Size(max = 70) List<@NotNull @Valid FranjaHorariaRequest> franjas) {

    /**
     * Comprueba que no hay dos franjas solapadas el mismo día. La base de datos también lo
     * garantiza (horario_sin_solape), pero validarlo aquí permite responder 400 con un mensaje claro.
     */
    @AssertTrue(message = "hay franjas que se solapan en el mismo día")
    public boolean isSinSolapes() {
        if (franjas == null || franjas.stream().anyMatch(HorarioRequest::incompleta)) {
            return true; // los datos incompletos ya los señalan las otras validaciones
        }
        for (int i = 0; i < franjas.size(); i++) {
            for (int j = i + 1; j < franjas.size(); j++) {
                if (franjas.get(i).seSolapaCon(franjas.get(j))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean incompleta(FranjaHorariaRequest franja) {
        return franja == null || franja.diaSemana() == null
                || franja.horaInicio() == null || franja.horaFin() == null;
    }
}
