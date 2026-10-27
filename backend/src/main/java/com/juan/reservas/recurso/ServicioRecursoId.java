package com.juan.reservas.recurso;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Clave compuesta de {@link ServicioRecurso}: la pareja (servicio, recurso).
 * JPA exige que una clave embebida sea Serializable e implemente equals y hashCode.
 */
@Embeddable
public class ServicioRecursoId implements Serializable {

    @Column(name = "servicio_id")
    private UUID servicioId;

    @Column(name = "recurso_id")
    private UUID recursoId;

    /** Requerido por JPA. */
    protected ServicioRecursoId() {
    }

    public ServicioRecursoId(UUID servicioId, UUID recursoId) {
        this.servicioId = servicioId;
        this.recursoId = recursoId;
    }

    public UUID getServicioId() {
        return servicioId;
    }

    public UUID getRecursoId() {
        return recursoId;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof ServicioRecursoId id
                && Objects.equals(servicioId, id.servicioId)
                && Objects.equals(recursoId, id.recursoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(servicioId, recursoId);
    }
}
