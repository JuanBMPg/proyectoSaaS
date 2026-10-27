package com.juan.reservas.disponibilidad;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

/**
 * Calcula los huecos libres de un día: a qué horas puede empezar un servicio y con qué recurso.
 * <p>
 * Es lógica pura: recibe todos los datos en una {@link SolicitudDisponibilidad} y no consulta
 * la base de datos ni el reloj. Así se puede probar a fondo con tests unitarios, incluidos los
 * cambios de hora. Es un {@code @Component} solo para poder inyectarla; no tiene estado.
 * <p>
 * Para cada recurso:
 * <ol>
 *   <li>Convierte sus franjas del día a instantes con la zona del negocio y une las contiguas.</li>
 *   <li>Recorre cada tramo en pasos de 15 min desde su inicio (09:00, 09:15...).</li>
 *   <li>Se queda con los inicios donde cabe el servicio más su margen entero dentro del tramo,
 *       sin solaparse con ningún intervalo ocupado y respetando la antelación mínima y máxima.</li>
 * </ol>
 * Comprobar cada inicio de la rejilla contra los ocupados da el mismo resultado que restar
 * primero los ocupados al horario y recorrer lo libre, con menos código.
 */
@Component
public class CalculadoraDisponibilidad {

    public List<Hueco> calcular(SolicitudDisponibilidad solicitud) {
        Instant primerInicio = solicitud.ahora().plus(solicitud.antelacionMinima());
        // Días de calendario en la zona del negocio: con un cambio de hora en medio,
        // 30 días no son exactamente 30 × 24 horas
        Instant ultimoInicio = solicitud.ahora().atZone(solicitud.zona())
                .plusDays(solicitud.antelacionMaximaDias())
                .toInstant();

        return solicitud.recursos().stream()
                .flatMap(agenda -> huecosDe(agenda, solicitud, primerInicio, ultimoInicio))
                .sorted(Comparator.comparing(Hueco::inicio).thenComparing(Hueco::recursoId))
                .toList();
    }

    private Stream<Hueco> huecosDe(AgendaRecurso agenda, SolicitudDisponibilidad solicitud,
            Instant primerInicio, Instant ultimoInicio) {
        List<Hueco> huecos = new ArrayList<>();
        for (Intervalo tramo : tramosDelDia(agenda.franjas(), solicitud)) {
            for (Instant inicio = tramo.inicio();
                    !inicio.plus(solicitud.bloque()).isAfter(tramo.fin());
                    inicio = inicio.plus(solicitud.paso())) {

                Intervalo bloque = new Intervalo(inicio, inicio.plus(solicitud.bloque()));
                boolean enPlazo = !inicio.isBefore(primerInicio) && !inicio.isAfter(ultimoInicio);
                boolean libre = agenda.ocupados().stream().noneMatch(bloque::seSolapaCon);

                if (enPlazo && libre) {
                    huecos.add(new Hueco(agenda.recursoId(), inicio, inicio.plus(solicitud.duracion())));
                }
            }
        }
        return huecos.stream();
    }

    /**
     * Franjas locales del día → intervalos de instantes, ordenados y con las contiguas unidas
     * (09:00-14:00 y 14:00-18:00 forman un único tramo: un servicio puede empezar a las 13:45).
     * <p>
     * La conversión usa {@link ZonedDateTime#of}, que resuelve los cambios de hora: una hora
     * que no existe (02:30 el último domingo de marzo) se adelanta a la siguiente válida (03:30),
     * y una hora repetida (02:30 el último domingo de octubre) toma el primer desfase.
     * Una franja que cae en la hora que no existe puede quedar vacía o invertida
     * (02:15-03:00 pasa a ser 03:15-03:00): esa franja se descarta.
     */
    private static List<Intervalo> tramosDelDia(List<FranjaLocal> franjas, SolicitudDisponibilidad solicitud) {
        List<Intervalo> ordenados = franjas.stream()
                .flatMap(franja -> aInstantes(franja, solicitud).stream())
                .sorted(Comparator.comparing(Intervalo::inicio))
                .toList();

        List<Intervalo> tramos = new ArrayList<>();
        for (Intervalo actual : ordenados) {
            int ultimo = tramos.size() - 1;
            if (ultimo >= 0 && !actual.inicio().isAfter(tramos.get(ultimo).fin())) {
                Intervalo anterior = tramos.get(ultimo);
                Instant fin = actual.fin().isAfter(anterior.fin()) ? actual.fin() : anterior.fin();
                tramos.set(ultimo, new Intervalo(anterior.inicio(), fin));
            } else {
                tramos.add(actual);
            }
        }
        return tramos;
    }

    private static Optional<Intervalo> aInstantes(FranjaLocal franja, SolicitudDisponibilidad solicitud) {
        Instant inicio = ZonedDateTime.of(solicitud.fecha(), franja.inicio(), solicitud.zona()).toInstant();
        Instant fin = ZonedDateTime.of(solicitud.fecha(), franja.fin(), solicitud.zona()).toInstant();
        return inicio.isBefore(fin) ? Optional.of(new Intervalo(inicio, fin)) : Optional.empty();
    }
}
