package com.juan.reservas.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

import com.juan.reservas.reserva.ReservaCreada;

class EmailReservaListenerTest {

    @Test
    void siElServidorDeCorreoFallaSeRegistraYNoPropagaElError() {
        NotificadorReservas notificador = mock(NotificadorReservas.class);
        doThrow(new MailSendException("servidor caído")).when(notificador).enviarConfirmacion(any(), anyString());
        EmailReservaListener listener = new EmailReservaListener(notificador);

        assertThatCode(() -> listener.alCrearReserva(new ReservaCreada(UUID.randomUUID(), "token")))
                .doesNotThrowAnyException();
    }

    @Test
    void elEventoNoMuestraElTokenAlConvertirseEnTexto() {
        ReservaCreada evento = new ReservaCreada(UUID.randomUUID(), "token-secreto");

        assertThat(evento.toString()).doesNotContain("token-secreto");
    }
}
