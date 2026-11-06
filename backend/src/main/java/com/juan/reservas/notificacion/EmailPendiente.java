package com.juan.reservas.notificacion;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.juan.reservas.comun.EntidadBase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Un email ya redactado esperando a salir (patrón outbox). Se guarda en la misma transacción
 * que el cambio que lo provoca y lo envía {@link EnviadorEmails}.
 * <p>
 * Las referencias son ids y no relaciones JPA: la cola no necesita cargar el negocio ni la
 * reserva para enviar, todo lo que hace falta ya está en la fila.
 */
@Entity
@Table(name = "email_pendiente")
public class EmailPendiente extends EntidadBase {

    /** Tras este número de fallos seguidos, el email se marca como FALLIDO. */
    public static final int MAX_INTENTOS = 5;
    /** Espera tras el primer fallo; se duplica en cada uno (1, 2, 4, 8 min). */
    static final Duration ESPERA_INICIAL = Duration.ofMinutes(1);

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "reserva_id", updatable = false)
    private @Nullable UUID reservaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private TipoEmail tipo;

    @Column(nullable = false, length = 160, updatable = false)
    private String para;

    @Column(name = "nombre_remitente", nullable = false, length = 120, updatable = false)
    private String nombreRemitente;

    @Column(name = "responder_a", length = 160, updatable = false)
    private @Nullable String responderA;

    @Column(nullable = false, length = 255, updatable = false)
    private String asunto;

    @Column(columnDefinition = "text")
    private @Nullable String texto;

    @Column(columnDefinition = "text")
    private @Nullable String html;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEmail estado = EstadoEmail.PENDIENTE;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "proximo_intento", nullable = false)
    private Instant proximoIntento;

    @Column(name = "ultimo_error", length = 500)
    private @Nullable String ultimoError;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "enviado_en")
    private @Nullable Instant enviadoEn;

    /** Requerido por JPA. */
    protected EmailPendiente() {
    }

    public EmailPendiente(UUID negocioId, @Nullable UUID reservaId, TipoEmail tipo, String nombreRemitente,
            @Nullable String responderA, EmailReserva email, Instant ahora) {
        this.negocioId = negocioId;
        this.reservaId = reservaId;
        this.tipo = tipo;
        this.para = email.para();
        this.nombreRemitente = nombreRemitente;
        this.responderA = responderA;
        this.asunto = email.asunto();
        this.texto = email.texto();
        this.html = email.html();
        this.proximoIntento = ahora;
        this.creadoEn = ahora;
    }

    /** Enviado: se vacía el cuerpo, que puede llevar el enlace secreto de la reserva. */
    public void marcarEnviado(Instant ahora) {
        this.estado = EstadoEmail.ENVIADO;
        this.enviadoEn = ahora;
        this.intentos++;
        this.ultimoError = null;
        this.texto = null;
        this.html = null;
    }

    /**
     * El servidor de correo ha fallado: se reintenta más tarde, cada vez esperando el doble
     * (para no insistir sobre un servidor caído), hasta {@link #MAX_INTENTOS}.
     */
    public void registrarFallo(String error, Instant ahora) {
        this.intentos++;
        this.ultimoError = error.length() > 500 ? error.substring(0, 500) : error;
        if (intentos >= MAX_INTENTOS) {
            this.estado = EstadoEmail.FALLIDO;
        } else {
            this.proximoIntento = ahora.plus(ESPERA_INICIAL.multipliedBy(1L << (intentos - 1)));
        }
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public @Nullable UUID getReservaId() {
        return reservaId;
    }

    public TipoEmail getTipo() {
        return tipo;
    }

    public String getPara() {
        return para;
    }

    public String getNombreRemitente() {
        return nombreRemitente;
    }

    public @Nullable String getResponderA() {
        return responderA;
    }

    public String getAsunto() {
        return asunto;
    }

    public @Nullable String getTexto() {
        return texto;
    }

    public @Nullable String getHtml() {
        return html;
    }

    public EstadoEmail getEstado() {
        return estado;
    }

    public int getIntentos() {
        return intentos;
    }

    public Instant getProximoIntento() {
        return proximoIntento;
    }

    public @Nullable String getUltimoError() {
        return ultimoError;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public @Nullable Instant getEnviadoEn() {
        return enviadoEn;
    }
}
