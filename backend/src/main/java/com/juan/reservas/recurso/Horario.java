package com.juan.reservas.recurso;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.juan.reservas.comun.EntidadBase;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Franja semanal en la que un recurso atiende (p. ej. lunes de 09:00 a 14:00).
 * Las horas son locales del negocio: la conversión a instantes UTC la hace la calculadora
 * de disponibilidad con la zona horaria del negocio, día a día (por los cambios de hora).
 */
@Entity
@Table(name = "horario")
public class Horario extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recurso_id", nullable = false, updatable = false)
    private Recurso recurso;

    @Convert(converter = DiaSemanaConverter.class)
    @Column(name = "dia_semana", nullable = false)
    private DayOfWeek diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    /** Requerido por JPA. */
    protected Horario() {
    }

    public Horario(Recurso recurso, DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFin) {
        this.recurso = recurso;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public DayOfWeek getDiaSemana() {
        return diaSemana;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }
}
