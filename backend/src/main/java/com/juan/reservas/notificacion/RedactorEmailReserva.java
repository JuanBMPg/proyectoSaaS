package com.juan.reservas.notificacion;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.juan.reservas.config.AppProperties;
import com.juan.reservas.negocio.Negocio;
import com.juan.reservas.reserva.EstadoReserva;
import com.juan.reservas.reserva.Reserva;

/**
 * Redacta los emails de una reserva. No envía nada: así se puede probar el contenido con un
 * test unitario, sin servidor de correo.
 * <p>
 * Todos usan la misma plantilla (resources/templates/email/reserva): saludo, un mensaje,
 * la ficha de la cita, el enlace a la reserva si se tiene y una nota final.
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

    /**
     * Al reservar: confirmada o, si el negocio confirma a mano, solicitud recibida.
     *
     * @param token token en claro del enlace; solo existe en memoria al crear la reserva
     */
    public EmailReserva confirmacion(Reserva reserva, String token) {
        Negocio negocio = reserva.getNegocio();
        String mensaje = reserva.getEstado() == EstadoReserva.CONFIRMADA
                ? "Tu cita está confirmada."
                : "Hemos recibido tu solicitud. " + negocio.getNombre() + " la confirmará y te avisaremos.";
        return redactar(reserva, "Tu cita en " + negocio.getNombre() + ": " + cuando(reserva), mensaje,
                enlaceReserva(token), "Ver tu reserva",
                "Guarda este email: con el enlace puedes consultar o cancelar tu cita.");
    }

    /**
     * Recordatorio previo a la cita. No lleva el enlace: de la reserva solo se guarda el hash
     * del token, así que el enlace en claro ya no existe. El cliente lo tiene en el email de
     * confirmación.
     */
    public EmailReserva recordatorio(Reserva reserva) {
        Negocio negocio = reserva.getNegocio();
        return redactar(reserva, "Recordatorio: tu cita en " + negocio.getNombre() + " " + cuando(reserva),
                "Te recordamos tu cita.", null, null,
                "Si no puedes venir, cancélala desde el enlace del email de confirmación para que otra persona "
                        + "pueda aprovechar la hora.");
    }

    /**
     * Aviso de cancelación, con un enlace a la página del negocio para reservar otra hora.
     *
     * @param porCliente   true si la canceló el propio cliente desde su enlace
     * @param eraSolicitud true si estaba pendiente: si la cancela el negocio, la ha rechazado
     */
    public EmailReserva cancelacion(Reserva reserva, boolean porCliente, boolean eraSolicitud) {
        Negocio negocio = reserva.getNegocio();
        String enlace = frontend() + "/" + negocio.getSlug();
        if (!porCliente && eraSolicitud) {
            return redactar(reserva, "Solicitud no aceptada: " + negocio.getNombre() + ", " + cuando(reserva),
                    negocio.getNombre() + " no puede atenderte a esa hora. Puedes elegir otra en su página.",
                    enlace, "Elegir otra hora", null);
        }
        String mensaje = porCliente
                ? "Tu cita se ha cancelado. Gracias por avisar: así otra persona puede aprovechar la hora."
                : negocio.getNombre() + " ha cancelado tu cita. Disculpa las molestias.";
        return redactar(reserva, "Cita cancelada: " + negocio.getNombre() + ", " + cuando(reserva), mensaje,
                enlace, "Reservar otra cita", null);
    }

    /**
     * El negocio ha confirmado una solicitud pendiente. Sin enlace: el token en claro solo
     * existió al crear la reserva; el cliente lo tiene en el primer email.
     */
    public EmailReserva confirmadaPorNegocio(Reserva reserva) {
        Negocio negocio = reserva.getNegocio();
        return redactar(reserva, "Cita confirmada: " + negocio.getNombre() + ", " + cuando(reserva),
                negocio.getNombre() + " ha confirmado tu cita. Te esperamos.", null, null,
                "Con el enlace del primer email puedes consultarla o cancelarla.");
    }

    private EmailReserva redactar(Reserva reserva, String asunto, String mensaje, @Nullable String enlace,
            @Nullable String textoEnlace, @Nullable String pie) {
        Negocio negocio = reserva.getNegocio();
        // Las horas, en la zona del negocio: es donde el cliente tiene que presentarse
        ZoneId zona = ZoneId.of(negocio.getZonaHoraria());
        ZonedDateTime inicio = reserva.getInicio().atZone(zona);
        ZonedDateTime fin = reserva.getFin().atZone(zona);

        Context datos = new Context(ESPANOL);
        datos.setVariable("cliente", reserva.getCliente().getNombre());
        datos.setVariable("mensaje", mensaje);
        datos.setVariable("servicio", reserva.getServicio().getNombre());
        datos.setVariable("dia", mayuscula(DIA.format(inicio)));
        datos.setVariable("horaInicio", HORA.format(inicio));
        datos.setVariable("horaFin", HORA.format(fin));
        datos.setVariable("recurso", reserva.getRecurso().getNombre());
        datos.setVariable("precio", euros(reserva.getServicio().getPrecioCentimos()));
        datos.setVariable("negocio", negocio.getNombre());
        datos.setVariable("direccion", negocio.getDireccion());
        datos.setVariable("telefono", negocio.getTelefono());
        datos.setVariable("enlace", enlace);
        datos.setVariable("textoEnlace", textoEnlace);
        datos.setVariable("pie", pie);

        return new EmailReserva(reserva.getCliente().getEmail(), asunto,
                plantillas.process("reserva-texto", datos),
                plantillas.process("reserva", datos));
    }

    /** "lunes 9 de noviembre, 10:00" en la zona del negocio. */
    private static String cuando(Reserva reserva) {
        ZonedDateTime inicio = reserva.getInicio().atZone(ZoneId.of(reserva.getNegocio().getZonaHoraria()));
        return DIA.format(inicio) + ", " + HORA.format(inicio);
    }

    private String enlaceReserva(String token) {
        return frontend() + "/reserva/" + token;
    }

    /** Dirección de la web sin la barra final, para no generar "//" en los enlaces. */
    private String frontend() {
        return props.frontendUrl().replaceAll("/+$", "");
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
