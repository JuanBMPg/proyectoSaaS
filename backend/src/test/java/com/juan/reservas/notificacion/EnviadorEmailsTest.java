package com.juan.reservas.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.juan.reservas.config.AppProperties;

import jakarta.mail.internet.MimeMessage;

/** Qué hace la cola cuando el servidor de correo falla, sin servidor ni base de datos. */
class EnviadorEmailsTest {

    private static final Instant AHORA = Instant.parse("2026-11-06T10:00:00Z");

    private final EmailPendienteRepository repository = mock(EmailPendienteRepository.class);
    private final JavaMailSender correo = mock(JavaMailSender.class);
    private EnviadorEmails enviador;

    @BeforeEach
    void preparar() {
        // Un MimeMessage real para que MimeMessageHelper pueda rellenarlo
        when(correo.createMimeMessage()).thenAnswer(i -> new JavaMailSenderImpl().createMimeMessage());
        AppProperties props = new AppProperties(new AppProperties.Cors(List.of("http://localhost")),
                new AppProperties.Mail("no-responder@reservas.local"), "http://localhost");
        enviador = new EnviadorEmails(repository, correo, props, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void unFalloDelServidorDejaElEmailEnLaColaParaReintentarYSigueConLosDemas() {
        EmailPendiente falla = email("falla@example.com");
        EmailPendiente sale = email("sale@example.com");
        when(repository.bloquearListos(any(), anyInt())).thenReturn(List.of(falla, sale));
        doThrow(new MailSendException("servidor caído")).doNothing().when(correo).send(any(MimeMessage.class));

        assertThatCode(enviador::enviarPendientes).doesNotThrowAnyException();

        assertThat(falla.getEstado()).isEqualTo(EstadoEmail.PENDIENTE);
        assertThat(falla.getIntentos()).isEqualTo(1);
        assertThat(falla.getProximoIntento()).isAfter(AHORA);
        assertThat(falla.getUltimoError()).contains("servidor caído");
        assertThat(sale.getEstado()).isEqualTo(EstadoEmail.ENVIADO);
    }

    @Test
    void unEmailEnviadoQuedaMarcadoConLaHora() {
        EmailPendiente email = email("juan@example.com");
        when(repository.bloquearListos(any(), anyInt())).thenReturn(List.of(email));
        doNothing().when(correo).send(any(MimeMessage.class));

        enviador.enviarPendientes();

        assertThat(email.getEstado()).isEqualTo(EstadoEmail.ENVIADO);
        assertThat(email.getEnviadoEn()).isEqualTo(AHORA);
    }

    private static EmailPendiente email(String para) {
        return new EmailPendiente(UUID.randomUUID(), UUID.randomUUID(), TipoEmail.RESERVA_CREADA, "Peluquería Lola",
                "lola@example.com", new EmailReserva(para, "Tu cita", "texto", "<p>html</p>"), AHORA);
    }
}
