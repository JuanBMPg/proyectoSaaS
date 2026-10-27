package com.juan.reservas.auth;

import java.time.Instant;

import com.juan.reservas.comun.EntidadBase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Refresh token emitido a un usuario. Solo se guarda el hash SHA-256 del token:
 * si alguien lee la base de datos no obtiene tokens válidos.
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 100)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(nullable = false)
    private boolean revocado = false;

    /** Requerido por JPA. */
    protected RefreshToken() {
    }

    public RefreshToken(Usuario usuario, String tokenHash, Instant expiraEn) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.expiraEn = expiraEn;
    }

    /** Válido si no se ha revocado y no ha caducado en el instante dado. */
    public boolean esValido(Instant ahora) {
        return !revocado && ahora.isBefore(expiraEn);
    }

    public void revocar() {
        this.revocado = true;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Instant getExpiraEn() {
        return expiraEn;
    }

    public boolean isRevocado() {
        return revocado;
    }
}
