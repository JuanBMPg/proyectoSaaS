package com.juan.reservas.auth;

import java.time.Instant;

import com.juan.reservas.comun.EntidadBase;
import com.juan.reservas.negocio.Negocio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Persona del negocio que entra al panel (propietario o empleado).
 * La columna recurso_id se mapeará cuando exista la entidad Recurso.
 */
@Entity
@Table(name = "usuario")
public class Usuario extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false, updatable = false)
    private Negocio negocio;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    /** Requerido por JPA. */
    protected Usuario() {
    }

    public Usuario(Negocio negocio, String email, String passwordHash, String nombre, Rol rol, Instant creadoEn) {
        this.negocio = negocio;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nombre = nombre;
        this.rol = rol;
        this.creadoEn = creadoEn;
    }

    /** Un usuario desactivado no puede iniciar sesión ni renovarla. */
    public void desactivar() {
        this.activo = false;
    }

    public Negocio getNegocio() {
        return negocio;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNombre() {
        return nombre;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
