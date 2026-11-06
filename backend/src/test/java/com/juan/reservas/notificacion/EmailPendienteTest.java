package com.juan.reservas.notificacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** Política de reintentos y limpieza de un email de la cola, sin base de datos. */
class EmailPendienteTest {

    private static final Instant AHORA = Instant.parse("2026-11-06T10:00:00Z");

    @Test
    void nuevoEstaPendienteYListoParaEnviarYa() {
        EmailPendiente email = email();

        assertThat(email.getEstado()).isEqualTo(EstadoEmail.PENDIENTE);
        assertThat(email.getProximoIntento()).isEqualTo(AHORA);
        assertThat(email.getIntentos()).isZero();
    }

    @Test
    void alEnviarseSeVaciaElCuerpoQuePuedeLlevarElEnlaceSecreto() {
        EmailPendiente email = email();

        email.marcarEnviado(AHORA.plusSeconds(5));

        assertThat(email.getEstado()).isEqualTo(EstadoEmail.ENVIADO);
        assertThat(email.getEnviadoEn()).isEqualTo(AHORA.plusSeconds(5));
        assertThat(email.getTexto()).isNull();
        assertThat(email.getHtml()).isNull();
        assertThat(email.getAsunto()).isEqualTo("Tu cita");
    }

    @Test
    void cadaFalloEsperaElDobleQueElAnterior() {
        EmailPendiente email = email();

        email.registrarFallo("timeout", AHORA);
        assertThat(email.getProximoIntento()).isEqualTo(AHORA.plus(Duration.ofMinutes(1)));
        email.registrarFallo("timeout", AHORA);
        assertThat(email.getProximoIntento()).isEqualTo(AHORA.plus(Duration.ofMinutes(2)));
        email.registrarFallo("timeout", AHORA);
        assertThat(email.getProximoIntento()).isEqualTo(AHORA.plus(Duration.ofMinutes(4)));

        assertThat(email.getEstado()).isEqualTo(EstadoEmail.PENDIENTE);
        assertThat(email.getIntentos()).isEqualTo(3);
        assertThat(email.getUltimoError()).isEqualTo("timeout");
    }

    @Test
    void trasElMaximoDeIntentosQuedaFallidoYConservaElCuerpoParaRevisarlo() {
        EmailPendiente email = email();

        for (int i = 0; i < EmailPendiente.MAX_INTENTOS; i++) {
            email.registrarFallo("servidor caído", AHORA);
        }

        assertThat(email.getEstado()).isEqualTo(EstadoEmail.FALLIDO);
        assertThat(email.getTexto()).isNotNull();
    }

    @Test
    void unErrorLargoSeRecortaAlTamanoDeLaColumna() {
        EmailPendiente email = email();

        email.registrarFallo("x".repeat(2000), AHORA);

        assertThat(email.getUltimoError()).hasSize(500);
    }

    private static EmailPendiente email() {
        return new EmailPendiente(UUID.randomUUID(), UUID.randomUUID(), TipoEmail.RESERVA_CREADA, "Peluquería Lola",
                null, new EmailReserva("juan@example.com", "Tu cita", "texto con enlace", "<p>html</p>"), AHORA);
    }
}
