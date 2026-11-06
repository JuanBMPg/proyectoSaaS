package com.juan.reservas.disponibilidad;

import java.util.List;
import java.util.Objects;

/**
 * Decide en qué franjas atiende un recurso un día concreto, combinando su horario semanal
 * con las excepciones de ese día. Es lógica pura, como la calculadora.
 * <p>
 * Reglas, de más a menos prioritaria:
 * <ol>
 *   <li>Un cierre (del recurso o de todo el negocio) gana siempre: ese día no se atiende.</li>
 *   <li>Si el recurso tiene horario especial ese día, se usa en lugar del semanal.</li>
 *   <li>Si no, y el negocio tiene horario especial ese día, se usa ese.</li>
 *   <li>Si no hay excepciones, el horario semanal.</li>
 * </ol>
 * Lo más concreto gana a lo general: "el 24 abrimos de 9 a 14" (negocio) y "ese día Marta
 * solo viene de 10 a 12" (recurso) dan 10-12 para Marta y 9-14 para los demás. Y cerrar es
 * más fuerte que abrir: si Marta está de vacaciones, un horario especial del negocio no la
 * hace trabajar.
 * <p>
 * Así la calculadora no cambia: recibe las franjas ya resueltas, como antes.
 */
public final class HorarioDelDia {

    private HorarioDelDia() {
    }

    public static List<FranjaLocal> franjas(List<FranjaLocal> semanales, List<ExcepcionDelDia> excepciones) {
        Objects.requireNonNull(semanales, "semanales");
        Objects.requireNonNull(excepciones, "excepciones");

        if (excepciones.stream().anyMatch(ExcepcionDelDia::esCierre)) {
            return List.of();
        }
        List<FranjaLocal> delRecurso = especiales(excepciones, false);
        if (!delRecurso.isEmpty()) {
            return delRecurso;
        }
        List<FranjaLocal> delNegocio = especiales(excepciones, true);
        if (!delNegocio.isEmpty()) {
            return delNegocio;
        }
        return List.copyOf(semanales);
    }

    private static List<FranjaLocal> especiales(List<ExcepcionDelDia> excepciones, boolean deTodoElNegocio) {
        return excepciones.stream()
                .filter(excepcion -> excepcion.deTodoElNegocio() == deTodoElNegocio)
                .map(ExcepcionDelDia::horario)
                .toList();
    }
}
