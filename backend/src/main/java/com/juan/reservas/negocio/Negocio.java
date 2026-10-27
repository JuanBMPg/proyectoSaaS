package com.juan.reservas.negocio;

import java.time.Instant;

import com.juan.reservas.comun.EntidadBase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * El cliente del SaaS (tenant). Todos los datos de negocio cuelgan de él.
 * <p>
 * Los valores por defecto se repiten aquí porque Hibernate inserta todas las columnas,
 * así que los DEFAULT de la tabla no llegan a aplicarse.
 */
@Entity
@Table(name = "negocio")
public class Negocio extends EntidadBase {

    public static final String ZONA_HORARIA_POR_DEFECTO = "Europe/Madrid";

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(name = "zona_horaria", nullable = false, length = 50)
    private String zonaHoraria = ZONA_HORARIA_POR_DEFECTO;

    @Column(length = 160)
    private String email;

    @Column(length = 30)
    private String telefono;

    @Column(length = 255)
    private String direccion;

    @Column(name = "etiqueta_recurso", nullable = false, length = 40)
    private String etiquetaRecurso = "Profesional";

    @Column(name = "antelacion_minima_min", nullable = false)
    private int antelacionMinimaMin = 120;

    @Column(name = "antelacion_maxima_dias", nullable = false)
    private int antelacionMaximaDias = 30;

    @Column(name = "plazo_cancelacion_horas", nullable = false)
    private int plazoCancelacionHoras = 24;

    @Column(name = "confirmacion_automatica", nullable = false)
    private boolean confirmacionAutomatica = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    /** Requerido por JPA. */
    protected Negocio() {
    }

    public Negocio(String nombre, String slug, Instant creadoEn) {
        this.nombre = nombre;
        this.slug = slug;
        this.creadoEn = creadoEn;
    }

    public String getNombre() {
        return nombre;
    }

    public String getSlug() {
        return slug;
    }

    public String getZonaHoraria() {
        return zonaHoraria;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getEtiquetaRecurso() {
        return etiquetaRecurso;
    }

    public int getAntelacionMinimaMin() {
        return antelacionMinimaMin;
    }

    public int getAntelacionMaximaDias() {
        return antelacionMaximaDias;
    }

    public int getPlazoCancelacionHoras() {
        return plazoCancelacionHoras;
    }

    public boolean isConfirmacionAutomatica() {
        return confirmacionAutomatica;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
