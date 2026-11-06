package com.juan.reservas.notificacion;

import java.io.UnsupportedEncodingException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.config.AppProperties;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Vacía la cola de emails (patrón outbox): cada pocos segundos toma los pendientes, los envía
 * y apunta el resultado. Si el servidor de correo falla, el email se queda en la cola y se
 * reintenta más tarde; la reserva que lo provocó no se ve afectada.
 * <p>
 * Garantía: "al menos una vez". Si la aplicación se cae después de enviar pero antes del
 * commit, ese email se enviará otra vez al arrancar. Un email repetido es mucho mejor que uno
 * perdido, y evitarlo del todo exigiría que el servidor de correo participara en la transacción.
 */
@Component
public class EnviadorEmails {

    private static final Logger log = LoggerFactory.getLogger(EnviadorEmails.class);

    /** Emails por ejecución: acota lo que dura la transacción que los tiene bloqueados. */
    static final int LOTE = 20;

    private final EmailPendienteRepository repository;
    private final JavaMailSender correo;
    private final AppProperties props;
    private final Clock clock;

    public EnviadorEmails(EmailPendienteRepository repository, JavaMailSender correo, AppProperties props,
            Clock clock) {
        this.repository = repository;
        this.correo = correo;
        this.props = props;
        this.clock = clock;
    }

    /** fixedDelay: la siguiente ejecución empieza cuando acaba la anterior, nunca se solapan. */
    @Scheduled(fixedDelayString = "${app.emails.intervalo:PT10S}", initialDelayString = "${app.emails.intervalo:PT10S}")
    @Transactional
    public void enviarPendientes() {
        List<EmailPendiente> listos = repository.bloquearListos(Instant.now(clock), LOTE);
        for (EmailPendiente email : listos) {
            enviar(email);
        }
    }

    private void enviar(EmailPendiente email) {
        try {
            correo.send(mensaje(email));
            email.marcarEnviado(Instant.now(clock));
        } catch (RuntimeException e) {
            // Cualquier fallo (servidor caído, dirección rechazada, mensaje mal formado) se anota
            // en ESTE email: si saliera la excepción, el rollback desharía también los ya enviados
            // del lote y un email defectuoso bloquearía la cola para siempre
            email.registrarFallo(String.valueOf(e.getMessage()), Instant.now(clock));
            if (email.getEstado() == EstadoEmail.FALLIDO) {
                log.error("Email {} ({}) descartado tras {} intentos", email.getId(), email.getTipo(),
                        email.getIntentos(), e);
            } else {
                log.warn("Fallo al enviar el email {} ({}); se reintentará a las {}", email.getId(),
                        email.getTipo(), email.getProximoIntento());
            }
        }
    }

    private MimeMessage mensaje(EmailPendiente email) {
        try {
            MimeMessage mensaje = correo.createMimeMessage();
            // multipart = true: el mensaje lleva la versión de texto y la HTML como alternativas
            MimeMessageHelper ayudante = new MimeMessageHelper(mensaje, true, "UTF-8");
            // Remitente técnico de la plataforma, con el nombre del negocio para que se reconozca
            ayudante.setFrom(props.mail().from(), email.getNombreRemitente());
            if (email.getResponderA() != null) {
                ayudante.setReplyTo(email.getResponderA());
            }
            ayudante.setTo(email.getPara());
            ayudante.setSubject(email.getAsunto());
            ayudante.setText(email.getTexto(), email.getHtml());
            return mensaje;
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailPreparationException("No se pudo preparar el email " + email.getId(), e);
        }
    }
}
