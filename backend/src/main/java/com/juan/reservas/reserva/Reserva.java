package com.juan.reservas.reserva;

import java.time.Instant;

import com.juan.reservas.cliente.Cliente;
import com.juan.reservas.comun.EntidadBase;
import com.juan.reservas.negocio.Negocio;
import com.juan.reservas.recurso.Recurso;
import com.juan.reservas.servicio.Servicio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Cita de un cliente: un servicio con un recurso, de inicio a fin.
 * <p>
 * {@code finBloqueo} es el fin más el margen del servicio. La restricción reserva_sin_solape
 * usa [inicio, finBloqueo) para que dos reservas activas del mismo recurso nunca se solapen.
 */
@Entity
@Table(name = "reserva")
public class Reserva extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false, updatable = false)
    private Negocio negocio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false, updatable = false)
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recurso_id", nullable = false, updatable = false)
    private Recurso recurso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fin;

    @Column(name = "fin_bloqueo", nullable = false)
    private Instant finBloqueo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    @Column(columnDefinition = "text")
    private String notas;

    /** Secreto del enlace con el que el cliente consulta o cancela su reserva sin cuenta. */
    @Column(name = "token_cancelacion", nullable = false, unique = true, length = 64)
    private String tokenCancelacion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    /** Requerido por JPA. */
    protected Reserva() {
    }

    public Reserva(Negocio negocio, Servicio servicio, Recurso recurso, Cliente cliente,
            Instant inicio, Instant fin, Instant finBloqueo, EstadoReserva estado,
            String tokenCancelacion, Instant creadoEn) {
        this.negocio = negocio;
        this.servicio = servicio;
        this.recurso = recurso;
        this.cliente = cliente;
        this.inicio = inicio;
        this.fin = fin;
        this.finBloqueo = finBloqueo;
        this.estado = estado;
        this.tokenCancelacion = tokenCancelacion;
        this.creadoEn = creadoEn;
    }

    public Negocio getNegocio() {
        return negocio;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public Recurso getRecurso() {
        return recurso;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFin() {
        return fin;
    }

    public Instant getFinBloqueo() {
        return finBloqueo;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public String getNotas() {
        return notas;
    }

    public String getTokenCancelacion() {
        return tokenCancelacion;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
