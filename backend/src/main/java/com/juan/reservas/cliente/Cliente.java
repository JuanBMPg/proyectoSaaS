package com.juan.reservas.cliente;

import java.time.Instant;

import com.juan.reservas.comun.EntidadBase;
import com.juan.reservas.negocio.Negocio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Persona que reserva desde la web pública. Pertenece a un negocio: el mismo email en dos
 * negocios son dos clientes distintos (UNIQUE (negocio_id, email) en la tabla).
 */
@Entity
@Table(name = "cliente")
public class Cliente extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false, updatable = false)
    private Negocio negocio;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(length = 30)
    private String telefono;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    /** Requerido por JPA. */
    protected Cliente() {
    }

    public Cliente(Negocio negocio, String nombre, String email, String telefono, Instant creadoEn) {
        this.negocio = negocio;
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.creadoEn = creadoEn;
    }

    /** Cada reserva trae los datos más recientes del cliente: se guardan esos. */
    public void actualizarContacto(String nombre, String telefono) {
        this.nombre = nombre;
        this.telefono = telefono;
    }

    public Negocio getNegocio() {
        return negocio;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
