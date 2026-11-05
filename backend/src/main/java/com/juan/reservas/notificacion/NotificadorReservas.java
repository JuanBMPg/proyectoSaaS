package com.juan.reservas.notificacion;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.comun.NoEncontradoException;
import com.juan.reservas.config.AppProperties;
import com.juan.reservas.negocio.Negocio;
import com.juan.reservas.reserva.Reserva;
import com.juan.reservas.reserva.ReservaRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/** Envía los emails a los clientes con Spring Mail (en local, al Mailpit del docker-compose). */
@Service
public class NotificadorReservas {

    private final JavaMailSender correo;
    private final ReservaRepository reservaRepository;
    private final RedactorEmailReserva redactor;
    private final AppProperties props;

    public NotificadorReservas(JavaMailSender correo, ReservaRepository reservaRepository,
            RedactorEmailReserva redactor, AppProperties props) {
        this.correo = correo;
        this.reservaRepository = reservaRepository;
        this.redactor = redactor;
        this.props = props;
    }

    /**
     * Carga la reserva ya confirmada en la base de datos y envía su email.
     * Transacción de solo lectura propia: se llama desde otro hilo, después del commit.
     *
     * @throws org.springframework.mail.MailException si el mensaje no se puede preparar o enviar
     */
    @Transactional(readOnly = true)
    public void enviarConfirmacion(UUID reservaId, String token) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new NoEncontradoException("Reserva no encontrada: " + reservaId));
        EmailReserva email = redactor.confirmacion(reserva, token);
        correo.send(mensaje(email, reserva.getNegocio()));
    }

    private MimeMessage mensaje(EmailReserva email, Negocio negocio) {
        try {
            MimeMessage mensaje = correo.createMimeMessage();
            // multipart = true: el mensaje lleva la versión de texto y la HTML como alternativas
            MimeMessageHelper ayudante = new MimeMessageHelper(mensaje, true, "UTF-8");
            // Remitente técnico de la plataforma, con el nombre del negocio para que se reconozca
            ayudante.setFrom(props.mail().from(), negocio.getNombre());
            if (negocio.getEmail() != null) {
                ayudante.setReplyTo(negocio.getEmail());
            }
            ayudante.setTo(email.para());
            ayudante.setSubject(email.asunto());
            ayudante.setText(email.texto(), email.html());
            return mensaje;
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailPreparationException("No se pudo preparar el email de la reserva", e);
        }
    }
}
