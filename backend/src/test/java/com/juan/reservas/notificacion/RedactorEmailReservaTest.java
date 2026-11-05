package com.juan.reservas.notificacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.juan.reservas.cliente.Cliente;
import com.juan.reservas.config.AppProperties;
import com.juan.reservas.negocio.Negocio;
import com.juan.reservas.recurso.Recurso;
import com.juan.reservas.recurso.TipoRecurso;
import com.juan.reservas.reserva.EstadoReserva;
import com.juan.reservas.reserva.Reserva;
import com.juan.reservas.servicio.Servicio;

/** Contenido del email de confirmación, sin servidor de correo ni Spring. */
class RedactorEmailReservaTest {

    private static final Instant CREADA = Instant.parse("2026-11-01T10:00:00Z");
    /** Lunes 9 de noviembre de 2026 a las 10:00 en Madrid (horario de invierno, UTC+1). */
    private static final Instant INICIO = Instant.parse("2026-11-09T09:00:00Z");

    private final RedactorEmailReserva redactor = new RedactorEmailReserva(
            new EmailConfig().motorPlantillasEmail(),
            new AppProperties(new AppProperties.Cors(List.of("http://localhost:4200")),
                    new AppProperties.Mail("no-responder@reservas.local"), "https://reservas.example.com/"));

    @Test
    void elAsuntoLlevaElNegocioElDiaYLaHoraLocal() {
        EmailReserva email = redactor.confirmacion(reserva("Juan", EstadoReserva.CONFIRMADA), "abc");

        assertThat(email.para()).isEqualTo("juan@example.com");
        assertThat(email.asunto()).isEqualTo("Tu cita en Peluquería Lola: lunes 9 de noviembre, 10:00");
    }

    @Test
    void elTextoLlevaLaCitaEnLaHoraDelNegocioYElEnlace() {
        String texto = redactor.confirmacion(reserva("Juan", EstadoReserva.CONFIRMADA), "abc").texto();

        assertThat(texto)
                .contains("Hola, Juan:")
                .contains("Tu cita está confirmada.")
                .contains("Lunes 9 de noviembre · 10:00 – 10:30")
                .contains("Con Marta · 15,00")
                .contains("Peluquería Lola · C/ Mayor 12")
                // Sin barra doble aunque la URL configurada termine en "/"
                .contains("Ver tu reserva: https://reservas.example.com/reserva/abc");
    }

    @Test
    void siLaReservaEstaPendienteDiceQueElNegocioLaConfirmara() {
        EmailReserva email = redactor.confirmacion(reserva("Juan", EstadoReserva.PENDIENTE), "abc");

        assertThat(email.texto()).contains("Hemos recibido tu solicitud").doesNotContain("está confirmada");
        assertThat(email.html()).contains("Hemos recibido tu solicitud");
    }

    @Test
    void elHtmlEnlazaALaReservaYEscapaLosDatosDelCliente() {
        String html = redactor.confirmacion(reserva("<b>Juan</b>", EstadoReserva.CONFIRMADA), "abc").html();

        assertThat(html)
                .contains("href=\"https://reservas.example.com/reserva/abc\"")
                .contains("&lt;b&gt;Juan&lt;/b&gt;")
                .doesNotContain("<b>Juan</b>");
    }

    private static Reserva reserva(String nombreCliente, EstadoReserva estado) {
        Negocio negocio = new Negocio("Peluquería Lola", "peluqueria-lola", CREADA);
        // Negocio aún no tiene método para editar la dirección (llegará con PUT /api/panel/negocio)
        ReflectionTestUtils.setField(negocio, "direccion", "C/ Mayor 12");
        Servicio corte = new Servicio(negocio, "Corte de pelo", null, 30, 10, 1500);
        Recurso marta = new Recurso(negocio, "Marta", TipoRecurso.PERSONA);
        Cliente cliente = new Cliente(negocio, nombreCliente, "juan@example.com", null, CREADA);
        return new Reserva(negocio, corte, marta, cliente, INICIO, INICIO.plusSeconds(30 * 60),
                INICIO.plusSeconds(40 * 60), estado, null, "hash", CREADA);
    }
}
