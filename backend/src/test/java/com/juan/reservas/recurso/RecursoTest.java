package com.juan.reservas.recurso;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.juan.reservas.negocio.Negocio;
import com.juan.reservas.servicio.Servicio;

/** Test unitario de la lógica de asignación de la entidad, sin base de datos. */
class RecursoTest {

    private final Negocio negocio = new Negocio("Peluquería Lola", "lola", Instant.parse("2026-06-02T08:00:00Z"));
    private final Recurso marta = new Recurso(negocio, "Marta", TipoRecurso.PERSONA);
    private final Servicio corte = new Servicio(negocio, "Corte", null, 30, 0, 1500);
    private final Servicio tinte = new Servicio(negocio, "Tinte", null, 60, 10, 3000);
    private final Servicio mechas = new Servicio(negocio, "Mechas", null, 90, 10, 4500);

    @Test
    void asignaLosServiciosIndicados() {
        marta.asignarServicios(List.of(corte, tinte));

        assertThat(marta.servicioIds()).containsExactlyInAnyOrder(corte.getId(), tinte.getId());
    }

    @Test
    void reasignarQuitaLosQueSobranYAñadeLosQueFaltan() {
        marta.asignarServicios(List.of(corte, tinte));

        marta.asignarServicios(List.of(tinte, mechas));

        assertThat(marta.servicioIds()).containsExactlyInAnyOrder(tinte.getId(), mechas.getId());
    }

    @Test
    void conservaLaMismaAsignacionSiElServicioSigue() {
        marta.asignarServicios(List.of(corte));
        ServicioRecurso asignacionOriginal = soloAsignacion();

        marta.asignarServicios(List.of(corte));

        // Mismo objeto: no se borra y se vuelve a crear (Hibernate rechazaría dos objetos con la misma clave)
        assertThat(soloAsignacion()).isSameAs(asignacionOriginal);
    }

    @Test
    void asignarUnaListaVaciaQuitaTodos() {
        marta.asignarServicios(List.of(corte, tinte));

        marta.asignarServicios(List.of());

        assertThat(marta.servicioIds()).isEmpty();
    }

    @Test
    void desactivarYReactivar() {
        marta.desactivar();
        assertThat(marta.isActivo()).isFalse();

        marta.cambiarActivo(true);
        assertThat(marta.isActivo()).isTrue();
    }

    private ServicioRecurso soloAsignacion() {
        return marta.asignaciones().iterator().next();
    }
}
