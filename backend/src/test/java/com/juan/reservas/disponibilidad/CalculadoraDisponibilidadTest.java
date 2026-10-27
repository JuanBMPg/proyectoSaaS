package com.juan.reservas.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios de la calculadora: sin Spring, sin base de datos y con fechas fijas.
 * Las horas esperadas se escriben en hora local de Madrid para que se lean como las vería el cliente.
 */
class CalculadoraDisponibilidadTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    /** Martes de verano (UTC+2). */
    private static final LocalDate MARTES = LocalDate.of(2026, 6, 2);
    /** El día anterior a medianoche: cualquier hora del martes cumple la antelación mínima. */
    private static final Instant LUNES_NOCHE = Instant.parse("2026-06-01T00:00:00Z");

    private static final UUID MARTA = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID LUCIA = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private final CalculadoraDisponibilidad calculadora = new CalculadoraDisponibilidad();

    @Nested
    class CasosBasicos {

        @Test
        void franjaSimpleDaUnInicioCada15MinutosMientrasCabeElServicio() {
            var huecos = calcular(MARTES, 30, 0, agenda(MARTA, franjas("09:00-11:00")));

            assertThat(horasLocales(huecos))
                    .containsExactly("09:00", "09:15", "09:30", "09:45", "10:00", "10:15", "10:30");
        }

        @Test
        void elHuecoTerminaConElServicioSinIncluirElMargen() {
            var huecos = calcular(MARTES, 30, 10, agenda(MARTA, franjas("09:00-11:00")));

            Hueco primero = huecos.getFirst();
            assertThat(Duration.between(primero.inicio(), primero.fin())).isEqualTo(Duration.ofMinutes(30));
        }

        @Test
        void elMargenTambienDebeCaberAntesDelCierre() {
            var huecos = calcular(MARTES, 30, 10, agenda(MARTA, franjas("09:00-11:00")));

            // 10:15 + 30 + 10 = 10:55 cabe; 10:30 + 40 = 11:10 no
            assertThat(horasLocales(huecos)).last().isEqualTo("10:15");
        }

        @Test
        void unServicioMasLargoQueLaFranjaNoTieneHuecos() {
            assertThat(calcular(MARTES, 90, 0, agenda(MARTA, franjas("09:00-10:00")))).isEmpty();
        }

        @Test
        void unRecursoSinHorarioEseDiaNoTieneHuecos() {
            assertThat(calcular(MARTES, 30, 0, agenda(MARTA, List.of()))).isEmpty();
        }

        @Test
        void elPasoEsConfigurable() {
            var solicitud = solicitud(MARTES, 30, 0, LUNES_NOCHE, Duration.ofMinutes(30),
                    agenda(MARTA, franjas("09:00-11:00")));

            assertThat(horasLocales(calculadora.calcular(solicitud)))
                    .containsExactly("09:00", "09:30", "10:00", "10:30");
        }
    }

    @Nested
    class Reservas {

        /** Es el ejemplo del diagrama del plan: reserva de 10:00 a 10:30 con 10 min de margen. */
        @Test
        void unaReservaConSuMargenParteLaFranjaEnDos() {
            var huecos = calcular(MARTES, 30, 10,
                    agenda(MARTA, franjas("09:00-14:00"), ocupado(MARTES, "10:00", "10:40")));

            assertThat(horasLocales(huecos)).hasSize(13)
                    .startsWith("09:00", "09:15", "10:45")
                    .doesNotContain("09:30", "09:45", "10:00", "10:15", "10:30")
                    .endsWith("13:15");
        }

        @Test
        void sePuedeEmpezarJustoCuandoTerminaElBloqueoDeOtraReserva() {
            var huecos = calcular(MARTES, 30, 0,
                    agenda(MARTA, franjas("09:00-11:00"), ocupado(MARTES, "09:00", "09:45")));

            assertThat(horasLocales(huecos)).first().isEqualTo("09:45");
        }

        @Test
        void sePuedeTerminarJustoCuandoEmpiezaOtraReserva() {
            var huecos = calcular(MARTES, 30, 0,
                    agenda(MARTA, franjas("09:00-11:00"), ocupado(MARTES, "10:00", "11:00")));

            assertThat(horasLocales(huecos)).containsExactly("09:00", "09:15", "09:30");
        }

        @Test
        void laReservaDeUnRecursoNoAfectaAOtro() {
            var huecos = calcular(MARTES, 30, 0,
                    agenda(MARTA, franjas("09:00-11:00"), ocupado(MARTES, "09:00", "10:00")),
                    agenda(LUCIA, franjas("09:00-11:00")));

            assertThat(huecos.stream().filter(h -> h.recursoId().equals(MARTA)).count()).isEqualTo(3);
            assertThat(huecos.stream().filter(h -> h.recursoId().equals(LUCIA)).count()).isEqualTo(7);
            // Ordenados por hora: a las 09:00 solo está libre Lucía
            assertThat(huecos.getFirst().recursoId()).isEqualTo(LUCIA);
        }
    }

    @Nested
    class Franjas {

        @Test
        void ningunHuecoCruzaLaPausaDeMediodia() {
            var huecos = calcular(MARTES, 60, 0, agenda(MARTA, franjas("09:00-14:00", "16:00-20:00")));

            assertThat(horasLocales(huecos))
                    .contains("13:00", "16:00")
                    .doesNotContain("13:15", "14:00", "15:45");
        }

        @Test
        void lasFranjasContiguasSeTratanComoUnTramoContinuo() {
            var huecos = calcular(MARTES, 30, 0, agenda(MARTA, franjas("14:00-18:00", "09:00-14:00")));

            // 13:45 + 30 = 14:15: cruza la unión de las dos franjas
            assertThat(horasLocales(huecos)).contains("13:45").hasSize(35);
        }
    }

    @Nested
    class Antelacion {

        @Test
        void respetaLaAntelacionMinima() {
            Instant ahora = local(MARTES, "09:40"); // + 120 min = 11:40
            var solicitud = solicitud(MARTES, 30, 0, ahora, SolicitudDisponibilidad.PASO_POR_DEFECTO,
                    agenda(MARTA, franjas("09:00-14:00")));

            assertThat(horasLocales(calculadora.calcular(solicitud))).first().isEqualTo("11:45");
        }

        @Test
        void respetaLaAntelacionMaximaHastaElMismoInstanteDentroDe30Dias() {
            Instant ahora = local(LocalDate.of(2026, 6, 1), "10:00");
            LocalDate dentroDe30Dias = LocalDate.of(2026, 7, 1);
            var solicitud = solicitud(dentroDe30Dias, 30, 0, ahora, SolicitudDisponibilidad.PASO_POR_DEFECTO,
                    agenda(MARTA, franjas("09:00-12:00")));

            assertThat(horasLocales(calculadora.calcular(solicitud)))
                    .containsExactly("09:00", "09:15", "09:30", "09:45", "10:00");
        }

        @Test
        void unDiaMasAllaDelPlazoNoTieneHuecos() {
            Instant ahora = local(LocalDate.of(2026, 6, 1), "10:00");
            var solicitud = solicitud(LocalDate.of(2026, 7, 2), 30, 0, ahora,
                    SolicitudDisponibilidad.PASO_POR_DEFECTO, agenda(MARTA, franjas("09:00-12:00")));

            assertThat(calculadora.calcular(solicitud)).isEmpty();
        }
    }

    @Nested
    class ZonasHorarias {

        @Test
        void enVeranoLas0900DeMadridSonLas0700Utc() {
            var huecos = calcular(MARTES, 30, 0, agenda(MARTA, franjas("09:00-09:30")));

            assertThat(huecos.getFirst().inicio()).isEqualTo(Instant.parse("2026-06-02T07:00:00Z"));
        }

        @Test
        void enInviernoLas0900DeMadridSonLas0800Utc() {
            LocalDate martesDeInvierno = LocalDate.of(2026, 1, 13);
            var huecos = calcular(martesDeInvierno, 30, 0, agenda(MARTA, franjas("09:00-09:30")));

            assertThat(huecos.getFirst().inicio()).isEqualTo(Instant.parse("2026-01-13T08:00:00Z"));
        }

        /** Último domingo de marzo: a las 02:00 pasan a ser las 03:00. El día tiene 23 horas. */
        @Test
        void diaDe23HorasNoOfreceLasHorasQueNoExisten() {
            LocalDate cambioDeMarzo = LocalDate.of(2026, 3, 29);
            var huecos = calcular(cambioDeMarzo, 30, 0, agenda(MARTA, franjas("01:00-05:00")));

            // La franja dura 3 horas reales: de 00:00Z a 03:00Z
            assertThat(horasLocales(huecos)).containsExactly(
                    "01:00", "01:15", "01:30", "01:45",
                    "03:00", "03:15", "03:30", "03:45", "04:00", "04:15", "04:30");
            assertThat(huecos.getFirst().inicio()).isEqualTo(Instant.parse("2026-03-29T00:00:00Z"));
            assertThat(huecos.getLast().inicio()).isEqualTo(Instant.parse("2026-03-29T02:30:00Z"));
        }

        /** Último domingo de octubre: a las 03:00 vuelven a ser las 02:00. El día tiene 25 horas. */
        @Test
        void diaDe25HorasOfreceDosVecesLaHoraRepetida() {
            LocalDate cambioDeOctubre = LocalDate.of(2026, 10, 25);
            var huecos = calcular(cambioDeOctubre, 30, 0, agenda(MARTA, franjas("01:00-04:00")));

            // La franja dura 4 horas reales: de 23:00Z del sábado a 03:00Z del domingo
            assertThat(horasLocales(huecos)).hasSize(15).containsExactly(
                    "01:00", "01:15", "01:30", "01:45",
                    "02:00", "02:15", "02:30", "02:45",
                    "02:00", "02:15", "02:30", "02:45",
                    "03:00", "03:15", "03:30");
            assertThat(huecos.stream().map(Hueco::inicio).distinct()).hasSize(15);
            assertThat(huecos).allSatisfy(h ->
                    assertThat(Duration.between(h.inicio(), h.fin())).isEqualTo(Duration.ofMinutes(30)));
        }

        @Test
        void unaFranjaQueEmpiezaEnLaHoraInexistenteSeAdelantaALaSiguienteValida() {
            LocalDate cambioDeMarzo = LocalDate.of(2026, 3, 29);
            var huecos = calcular(cambioDeMarzo, 30, 0, agenda(MARTA, franjas("02:30-05:00")));

            assertThat(horasLocales(huecos)).first().isEqualTo("03:30");
        }

        @Test
        void unaFranjaQueDesapareceConElCambioDeHoraNoDaHuecosNiFalla() {
            LocalDate cambioDeMarzo = LocalDate.of(2026, 3, 29);

            // 02:15 pasa a 03:15, que es posterior al fin (03:00)
            assertThat(calcular(cambioDeMarzo, 15, 0, agenda(MARTA, franjas("02:15-03:00")))).isEmpty();
        }
    }

    @Nested
    class Propiedades {

        /**
         * Con datos aleatorios (semilla fija, así el test es reproducible) comprueba lo que
         * debe cumplirse siempre: todo hueco cabe en una franja, no pisa nada ocupado y cae
         * en la rejilla de 15 minutos desde el inicio de su franja.
         */
        @Test
        void ningunHuecoPisaUnaReservaNiSaleDeSuFranja() {
            Random aleatorio = new Random(20261027L);
            int huecosComprobados = 0;

            for (int caso = 0; caso < 300; caso++) {
                List<FranjaLocal> franjas = franjasAleatorias(aleatorio);
                List<Intervalo> ocupados = ocupadosAleatorios(aleatorio);
                int duracion = 15 + 5 * aleatorio.nextInt(22);
                int margen = 5 * aleatorio.nextInt(7);

                var huecos = calcular(MARTES, duracion, margen, new AgendaRecurso(MARTA, franjas, ocupados));

                for (Hueco hueco : huecos) {
                    Intervalo bloque = new Intervalo(hueco.inicio(),
                            hueco.inicio().plus(Duration.ofMinutes(duracion + margen)));
                    assertThat(ocupados).noneMatch(bloque::seSolapaCon);
                    assertThat(franjas).anySatisfy(franja -> {
                        Instant inicioFranja = local(MARTES, franja.inicio().toString());
                        assertThat(bloque.inicio()).isAfterOrEqualTo(inicioFranja);
                        assertThat(bloque.fin()).isBeforeOrEqualTo(local(MARTES, franja.fin().toString()));
                        assertThat(Duration.between(inicioFranja, bloque.inicio()).toMinutes() % 15).isZero();
                    });
                    huecosComprobados++;
                }
            }
            // Que el test compruebe algo de verdad, no solo listas vacías
            assertThat(huecosComprobados).isGreaterThan(1000);
        }

        /** Entre 1 y 3 franjas separadas al menos 15 min (no contiguas), entre las 07:00 y las 22:00. */
        private List<FranjaLocal> franjasAleatorias(Random aleatorio) {
            List<FranjaLocal> franjas = new ArrayList<>();
            int cuantas = 1 + aleatorio.nextInt(3);
            int minuto = 7 * 60 + 15 * aleatorio.nextInt(8);
            for (int i = 0; i < cuantas && minuto < 21 * 60; i++) {
                int fin = Math.min(22 * 60, minuto + 60 + 15 * aleatorio.nextInt(20));
                franjas.add(new FranjaLocal(LocalTime.ofSecondOfDay(minuto * 60L), LocalTime.ofSecondOfDay(fin * 60L)));
                minuto = fin + 15 + 15 * aleatorio.nextInt(8);
            }
            return franjas;
        }

        /** Hasta 5 reservas de 10 a 120 min a cualquier minuto del día (también fuera de horario). */
        private List<Intervalo> ocupadosAleatorios(Random aleatorio) {
            List<Intervalo> ocupados = new ArrayList<>();
            int cuantas = aleatorio.nextInt(6);
            for (int i = 0; i < cuantas; i++) {
                Instant inicio = local(MARTES, "06:00").plus(Duration.ofMinutes(aleatorio.nextInt(16 * 60)));
                ocupados.add(new Intervalo(inicio, inicio.plus(Duration.ofMinutes(10 + aleatorio.nextInt(111)))));
            }
            return ocupados;
        }
    }

    @Nested
    class ValidacionDeLaSolicitud {

        @Test
        void rechazaUnaDuracionCero() {
            assertThatThrownBy(() -> calcular(MARTES, 0, 0, agenda(MARTA, franjas("09:00-10:00"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("duracion");
        }

        @Test
        void rechazaUnMargenNegativo() {
            assertThatThrownBy(() -> calcular(MARTES, 30, -5, agenda(MARTA, franjas("09:00-10:00"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("margen");
        }

        @Test
        void rechazaUnaFranjaInvertida() {
            assertThatThrownBy(() -> franjas("14:00-09:00")).isInstanceOf(IllegalArgumentException.class);
        }
    }

    // --- Utilidades ---

    private List<Hueco> calcular(LocalDate fecha, int duracionMin, int margenMin, AgendaRecurso... agendas) {
        // Para fechas lejanas (cambios de hora), "ahora" es la víspera: así se cumplen las dos antelaciones
        Instant ahora = fecha.equals(MARTES) ? LUNES_NOCHE : local(fecha.minusDays(1), "00:00");
        return calculadora.calcular(
                solicitud(fecha, duracionMin, margenMin, ahora, SolicitudDisponibilidad.PASO_POR_DEFECTO, agendas));
    }

    private static SolicitudDisponibilidad solicitud(LocalDate fecha, int duracionMin, int margenMin,
            Instant ahora, Duration paso, AgendaRecurso... agendas) {
        return new SolicitudDisponibilidad(fecha, MADRID, Duration.ofMinutes(duracionMin),
                Duration.ofMinutes(margenMin), ahora, Duration.ofMinutes(120), 30, paso, List.of(agendas));
    }

    private static AgendaRecurso agenda(UUID recurso, List<FranjaLocal> franjas, Intervalo... ocupados) {
        return new AgendaRecurso(recurso, franjas, List.of(ocupados));
    }

    /** "09:00-14:00" → FranjaLocal. */
    private static List<FranjaLocal> franjas(String... textos) {
        return Arrays.stream(textos)
                .map(texto -> texto.split("-"))
                .map(partes -> new FranjaLocal(LocalTime.parse(partes[0]), LocalTime.parse(partes[1])))
                .toList();
    }

    private static Intervalo ocupado(LocalDate fecha, String inicio, String fin) {
        return new Intervalo(local(fecha, inicio), local(fecha, fin));
    }

    private static Instant local(LocalDate fecha, String hora) {
        return ZonedDateTime.of(fecha, LocalTime.parse(hora), MADRID).toInstant();
    }

    private static List<String> horasLocales(List<Hueco> huecos) {
        return huecos.stream().map(h -> h.inicio().atZone(MADRID).toLocalTime().toString()).toList();
    }
}
