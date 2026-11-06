package com.juan.reservas.reserva;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class ReservaCreadaTest {

    @Test
    void elEventoNoMuestraElTokenAlConvertirseEnTexto() {
        ReservaCreada evento = new ReservaCreada(UUID.randomUUID(), "token-secreto");

        assertThat(evento.toString()).doesNotContain("token-secreto");
    }
}
