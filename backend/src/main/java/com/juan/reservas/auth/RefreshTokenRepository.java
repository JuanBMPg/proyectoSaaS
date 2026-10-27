package com.juan.reservas.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Bloquea la fila (SELECT ... FOR UPDATE) para que dos peticiones simultáneas con el
     * mismo token no puedan rotarlo las dos: la segunda espera y lo encuentra ya revocado.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
