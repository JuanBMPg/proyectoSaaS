package com.juan.reservas.notificacion;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.juan.reservas.config.AppProperties;
import com.juan.reservas.negocio.Negocio;
import com.juan.reservas.reserva.EstadoReserva;
import com.juan.reservas.reserva.Reserva;

/**
 * Redacta el email de confirmación de una reserva. No envía nada: así se puede probar el
 * contenido con un test unitario, sin servidor de correo.
 */
@Component
public class RedactorEmailReserva {

    private static final Locale ESPANOL = Locale.of("es", "ES");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ESPANOL);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", ESPANOL);

    private final TemplateEngine plantillas;
    private final AppProperties props;

    public RedactorEmailReserva(@Qualifier("motorPlantillasEmail") TemplateEngine plantillas, AppProperties props) {
        this.plantillas = plantillas;
        this.props = props;
    }

    /** @param token token en claro del enlace; solo existe en memoria al crear la reserva */
    public EmailReserva confirmacion(Reserva reserva, String token) {
        Negocio negocio = reserva.getNegocio();
        // Las horas, en la zona del negocio: es donde el cliente tiene que presentarse
        ZoneId zona = ZoneId.of(negocio.getZonaHoraria());
        ZonedDateTime inicio = reserva.getInicio().atZone(zona);
        ZonedDateTime fin = reserva.getFin().atZone(zona);
        String dia = DIA.format(inicio);

        Context datos = new Context(ESPANOL);
        datos.setVariable("cliente", reserva.getCliente().getNombre());
        datos.setVariable("confirmada", reserva.getEstado() == EstadoReserva.CONFIRMADA);
        datos.setVariable("servicio", reserva.getServicio().getNombre());
        datos.setVariable("dia", mayuscula(dia));
        datos.setVariable("horaInicio", HORA.format(inicio));
        datos.setVariable("horaFin", HORA.format(fin));
        datos.setVariable("recurso", reserva.getRecurso().getNombre());
        datos.setVariable("precio", euros(reserva.getServicio().getPrecioCentimos()));
        datos.setVariable("negocio", negocio.getNombre());
        datos.setVariable("direccion", negocio.getDireccion());
        datos.setVariable("telefono", negocio.getTelefono());
        datos.setVariable("enlace", props.frontendUrl().replaceAll("/+$", "") + "/reserva/" + token);

        String asunto = "Tu cita en " + negocio.getNombre() + ": " + dia + ", " + HORA.format(inicio);
        return new EmailReserva(reserva.getCliente().getEmail(), asunto,
                plantillas.process("reserva-confirmada-texto", datos),
                plantillas.process("reserva-confirmada", datos));
    }

    private static String euros(int centimos) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(ESPANOL);
        formato.setCurrency(Currency.getInstance("EUR"));
        return formato.format(centimos / 100.0);
    }

    private static String mayuscula(String texto) {
        return texto.substring(0, 1).toUpperCase(ESPANOL) + texto.substring(1);
    }
}
