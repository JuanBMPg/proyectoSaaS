package com.juan.reservas.comun;

import java.util.Objects;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Persistable;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;

/**
 * Base de todas las entidades: identificador UUID generado en la aplicación.
 * <p>
 * Generar el id en Java (y no con una secuencia) evita exponer cuántos registros hay
 * y permite conocer el id antes de guardar. El problema es que Spring Data considera
 * "existente" cualquier entidad con id no nulo y haría un SELECT + merge en lugar de un
 * INSERT. Implementar {@link Persistable} le indica explícitamente si la entidad es nueva.
 */
@MappedSuperclass
public abstract class EntidadBase implements Persistable<UUID> {

    @Id
    private UUID id = UUID.randomUUID();

    @Transient
    private boolean nueva = true;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return nueva;
    }

    @PostPersist
    @PostLoad
    void marcarComoPersistida() {
        this.nueva = false;
    }

    /** Dos entidades son iguales si son del mismo tipo y tienen el mismo id. */
    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        // Hibernate.getClass ignora los proxies de carga perezosa
        if (otro == null || Hibernate.getClass(this) != Hibernate.getClass(otro)) {
            return false;
        }
        return Objects.equals(id, ((EntidadBase) otro).getId());
    }

    @Override
    public int hashCode() {
        // Seguro porque el id se asigna al construir la entidad y nunca cambia
        return Objects.hashCode(getId());
    }
}
