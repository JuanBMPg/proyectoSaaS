package com.juan.reservas.excepcion;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.juan.reservas.comun.EntidadBase;
import com.juan.reservas.negocio.Negocio;
import com.juan.reservas.recurso.Recurso;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Excepción al horario semanal durante unos días: cierre (vacaciones, festivo) u horario
 * especial. Fechas y horas son locales del negocio, como el horario semanal.
 */
@Entity
@Table(name = "excepcion")
public class Excepcion extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false, updatable = false)
    private Negocio negocio;

    /** Null: afecta a todo el negocio. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurso_id", updatable = false)
    private @Nullable Recurso recurso;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    /** Incluida. */
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    /** Null (con horaFin null): cerrado todo el día. */
    @Column(name = "hora_inicio")
    private @Nullable LocalTime horaInicio;

    @Column(name = "hora_fin")
    private @Nullable LocalTime horaFin;

    @Column(nullable = false, length = 120)
    private String motivo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    /** Requerido por JPA. */
    protected Excepcion() {
    }

    public Excepcion(Negocio negocio, @Nullable Recurso recurso, LocalDate fechaInicio, LocalDate fechaFin,
            @Nullable LocalTime horaInicio, @Nullable LocalTime horaFin, String motivo, Instant creadoEn) {
        this.negocio = negocio;
        this.recurso = recurso;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.motivo = motivo;
        this.creadoEn = creadoEn;
    }

    public boolean esCierre() {
        return horaInicio == null;
    }

    /** Si esta excepción afecta a ese recurso (por ser suya o de todo el negocio). */
    public boolean afectaA(UUID recursoId) {
        return recurso == null || recurso.getId().equals(recursoId);
    }

    public Negocio getNegocio() {
        return negocio;
    }

    public @Nullable Recurso getRecurso() {
        return recurso;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public @Nullable LocalTime getHoraInicio() {
        return horaInicio;
    }

    public @Nullable LocalTime getHoraFin() {
        return horaFin;
    }

    public String getMotivo() {
        return motivo;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
