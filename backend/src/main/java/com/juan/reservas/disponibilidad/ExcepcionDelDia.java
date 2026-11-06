package com.juan.reservas.disponibilidad;

import org.jspecify.annotations.Nullable;

/**
 * Una excepción del horario que afecta a un recurso en un día concreto.
 *
 * @param deTodoElNegocio true si es del negocio entero (un festivo); false si es solo del recurso
 * @param horario         franja en la que se atiende ese día; null significa cerrado todo el día
 */
public record ExcepcionDelDia(boolean deTodoElNegocio, @Nullable FranjaLocal horario) {

    public static ExcepcionDelDia cierre(boolean deTodoElNegocio) {
        return new ExcepcionDelDia(deTodoElNegocio, null);
    }

    public static ExcepcionDelDia horarioEspecial(boolean deTodoElNegocio, FranjaLocal horario) {
        return new ExcepcionDelDia(deTodoElNegocio, horario);
    }

    public boolean esCierre() {
        return horario == null;
    }
}
