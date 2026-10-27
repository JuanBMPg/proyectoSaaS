package com.juan.reservas.recurso;

import java.util.Objects;
import java.util.UUID;

import com.juan.reservas.servicio.Servicio;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/**
 * Asignación "este recurso presta este servicio" (tabla servicio_recurso).
 * <p>
 * Es una entidad explícita, y no un {@code @ManyToMany}, porque la tabla tiene una columna
 * más que las dos claves: negocio_id, que la base de datos usa en sus FKs compuestas para
 * impedir enlazar un servicio y un recurso de negocios distintos. Un {@code @ManyToMany}
 * solo sabe escribir las dos claves de la tabla intermedia.
 */
@Entity
@Table(name = "servicio_recurso")
public class ServicioRecurso {

    @EmbeddedId
    private ServicioRecursoId id;

    // @MapsId: las claves del id embebido se rellenan a partir de estas relaciones
    @MapsId("recursoId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurso_id")
    private Recurso recurso;

    @MapsId("servicioId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    /** Requerido por JPA. */
    protected ServicioRecurso() {
    }

    ServicioRecurso(Recurso recurso, Servicio servicio) {
        this.id = new ServicioRecursoId(servicio.getId(), recurso.getId());
        this.recurso = recurso;
        this.servicio = servicio;
        this.negocioId = recurso.getNegocio().getId();
    }

    public UUID getServicioId() {
        return id.getServicioId();
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof ServicioRecurso asignacion && Objects.equals(id, asignacion.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
