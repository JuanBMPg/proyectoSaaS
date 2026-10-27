package com.juan.reservas.recurso;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.juan.reservas.comun.EntidadBase;
import com.juan.reservas.negocio.Negocio;
import com.juan.reservas.servicio.Servicio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/** Quién o qué atiende las reservas: una persona o una cosa. */
@Entity
@Table(name = "recurso")
public class Recurso extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false, updatable = false)
    private Negocio negocio;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRecurso tipo;

    @Column(nullable = false)
    private boolean activo = true;

    /** orphanRemoval: quitar una asignación del conjunto borra su fila de servicio_recurso. */
    @OneToMany(mappedBy = "recurso", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ServicioRecurso> asignaciones = new HashSet<>();

    /** Requerido por JPA. */
    protected Recurso() {
    }

    public Recurso(Negocio negocio, String nombre, TipoRecurso tipo) {
        this.negocio = negocio;
        actualizar(nombre, tipo);
    }

    public void actualizar(String nombre, TipoRecurso tipo) {
        this.nombre = nombre;
        this.tipo = tipo;
    }

    /**
     * Sustituye los servicios que presta el recurso.
     * <p>
     * Solo se quitan las asignaciones que sobran y se añaden las que faltan, en lugar de
     * vaciar y volver a rellenar. Si se vaciara, la asignación eliminada seguiría en el
     * contexto de persistencia hasta el flush, y al añadir otra con la misma clave
     * (servicio, recurso) Hibernate lanzaría NonUniqueObjectException.
     */
    public void asignarServicios(Collection<Servicio> servicios) {
        Set<UUID> nuevos = servicios.stream().map(Servicio::getId).collect(Collectors.toSet());
        Set<UUID> actuales = servicioIds();

        asignaciones.removeIf(asignacion -> !nuevos.contains(asignacion.getServicioId()));
        servicios.stream()
                .filter(servicio -> !actuales.contains(servicio.getId()))
                .forEach(servicio -> asignaciones.add(new ServicioRecurso(this, servicio)));
    }

    /** Solo lectura y visible solo en el paquete: las asignaciones se cambian con asignarServicios. */
    Set<ServicioRecurso> asignaciones() {
        return Collections.unmodifiableSet(asignaciones);
    }

    public Set<UUID> servicioIds() {
        return asignaciones.stream().map(ServicioRecurso::getServicioId).collect(Collectors.toSet());
    }

    /** Un recurso no se borra: las reservas pasadas siguen apuntando a él. */
    public void desactivar() {
        cambiarActivo(false);
    }

    public void cambiarActivo(boolean activo) {
        this.activo = activo;
    }

    public Negocio getNegocio() {
        return negocio;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoRecurso getTipo() {
        return tipo;
    }

    public boolean isActivo() {
        return activo;
    }
}
