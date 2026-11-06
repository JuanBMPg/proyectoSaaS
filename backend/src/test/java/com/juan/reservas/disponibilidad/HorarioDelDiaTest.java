package com.juan.reservas.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class HorarioDelDiaTest {

    private static final List<FranjaLocal> SEMANAL = List.of(franja("09:00", "14:00"), franja("16:00", "20:00"));

    @Test
    void sinExcepcionesSeUsaElHorarioSemanal() {
        assertThat(HorarioDelDia.franjas(SEMANAL, List.of())).isEqualTo(SEMANAL);
    }

    @Test
    void unCierreDelRecursoDejaElDiaSinFranjas() {
        assertThat(HorarioDelDia.franjas(SEMANAL, List.of(ExcepcionDelDia.cierre(false)))).isEmpty();
    }

    @Test
    void unCierreDelNegocioDejaElDiaSinFranjas() {
        assertThat(HorarioDelDia.franjas(SEMANAL, List.of(ExcepcionDelDia.cierre(true)))).isEmpty();
    }

    @Test
    void unHorarioEspecialSustituyeAlSemanal() {
        var especial = ExcepcionDelDia.horarioEspecial(true, franja("10:00", "13:00"));

        assertThat(HorarioDelDia.franjas(SEMANAL, List.of(especial))).containsExactly(franja("10:00", "13:00"));
    }

    @Test
    void unHorarioEspecialHaceAtenderUnDiaSinHorarioSemanal() {
        var especial = ExcepcionDelDia.horarioEspecial(false, franja("10:00", "14:00"));

        assertThat(HorarioDelDia.franjas(List.of(), List.of(especial))).containsExactly(franja("10:00", "14:00"));
    }

    @Test
    void elHorarioEspecialDelRecursoGanaAlDelNegocio() {
        var delNegocio = ExcepcionDelDia.horarioEspecial(true, franja("09:00", "14:00"));
        var delRecurso = ExcepcionDelDia.horarioEspecial(false, franja("10:00", "12:00"));

        assertThat(HorarioDelDia.franjas(SEMANAL, List.of(delNegocio, delRecurso)))
                .containsExactly(franja("10:00", "12:00"));
    }

    @Test
    void variosHorariosEspecialesDelMismoNivelSeSuman() {
        var manana = ExcepcionDelDia.horarioEspecial(false, franja("09:00", "11:00"));
        var tarde = ExcepcionDelDia.horarioEspecial(false, franja("17:00", "19:00"));

        assertThat(HorarioDelDia.franjas(SEMANAL, List.of(manana, tarde)))
                .containsExactly(franja("09:00", "11:00"), franja("17:00", "19:00"));
    }

    @Test
    void cerrarGanaAAbrirAunqueElCierreSeaDelRecursoYElHorarioDelNegocio() {
        var horarioDelNegocio = ExcepcionDelDia.horarioEspecial(true, franja("09:00", "14:00"));
        var vacaciones = ExcepcionDelDia.cierre(false);

        assertThat(HorarioDelDia.franjas(SEMANAL, List.of(horarioDelNegocio, vacaciones))).isEmpty();
    }

    @Test
    void cerrarGanaAunqueElRecursoTengaHorarioEspecial() {
        var horarioDelRecurso = ExcepcionDelDia.horarioEspecial(false, franja("10:00", "12:00"));
        var festivo = ExcepcionDelDia.cierre(true);

        assertThat(HorarioDelDia.franjas(SEMANAL, List.of(horarioDelRecurso, festivo))).isEmpty();
    }

    private static FranjaLocal franja(String inicio, String fin) {
        return new FranjaLocal(LocalTime.parse(inicio), LocalTime.parse(fin));
    }
}
