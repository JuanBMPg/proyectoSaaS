package com.juan.reservas.reserva;

import static com.juan.reservas.reserva.EstadoReserva.CANCELADA;
import static com.juan.reservas.reserva.EstadoReserva.COMPLETADA;
import static com.juan.reservas.reserva.EstadoReserva.CONFIRMADA;
import static com.juan.reservas.reserva.EstadoReserva.NO_PRESENTADO;
import static com.juan.reservas.reserva.EstadoReserva.PENDIENTE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class EstadoReservaTest {

    @Test
    void unaPendienteSeConfirmaOSeCancela() {
        assertThat(permitidos(PENDIENTE)).containsExactlyInAnyOrder(CONFIRMADA, CANCELADA);
    }

    @Test
    void unaConfirmadaSeCancelaSeCompletaOSeMarcaNoPresentado() {
        assertThat(permitidos(CONFIRMADA)).containsExactlyInAnyOrder(CANCELADA, COMPLETADA, NO_PRESENTADO);
    }

    @Test
    void completadaYNoPresentadoSePuedenCorregirEntreSi() {
        assertThat(permitidos(COMPLETADA)).containsExactly(NO_PRESENTADO);
        assertThat(permitidos(NO_PRESENTADO)).containsExactly(COMPLETADA);
    }

    @Test
    void unaCanceladaNoSeReactiva() {
        assertThat(permitidos(CANCELADA)).isEmpty();
    }

    private static EstadoReserva[] permitidos(EstadoReserva desde) {
        return Arrays.stream(EstadoReserva.values()).filter(desde::puedePasarA).toArray(EstadoReserva[]::new);
    }
}
