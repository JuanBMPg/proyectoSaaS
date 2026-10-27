package com.juan.reservas.servicio;

import com.juan.reservas.comun.EntidadBase;
import com.juan.reservas.negocio.Negocio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Lo que se reserva: dura un tiempo, deja un margen posterior y tiene un precio. */
@Entity
@Table(name = "servicio")
public class Servicio extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false, updatable = false)
    private Negocio negocio;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(name = "duracion_min", nullable = false)
    private int duracionMin;

    /** Minutos que el recurso queda bloqueado después del servicio (limpieza, preparación...). */
    @Column(name = "margen_min", nullable = false)
    private int margenMin;

    /** En céntimos para no arrastrar errores de redondeo de los decimales. */
    @Column(name = "precio_centimos", nullable = false)
    private int precioCentimos;

    @Column(nullable = false)
    private boolean activo = true;

    /** Requerido por JPA. */
    protected Servicio() {
    }

    public Servicio(Negocio negocio, String nombre, String descripcion,
            int duracionMin, int margenMin, int precioCentimos) {
        this.negocio = negocio;
        actualizar(nombre, descripcion, duracionMin, margenMin, precioCentimos);
    }

    public void actualizar(String nombre, String descripcion, int duracionMin, int margenMin, int precioCentimos) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.duracionMin = duracionMin;
        this.margenMin = margenMin;
        this.precioCentimos = precioCentimos;
    }

    /** Un servicio no se borra: las reservas pasadas siguen apuntando a él. */
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

    public String getDescripcion() {
        return descripcion;
    }

    public int getDuracionMin() {
        return duracionMin;
    }

    public int getMargenMin() {
        return margenMin;
    }

    public int getPrecioCentimos() {
        return precioCentimos;
    }

    public boolean isActivo() {
        return activo;
    }
}
