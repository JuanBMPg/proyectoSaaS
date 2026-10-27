package com.juan.reservas.cliente;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    /** El email identifica al cliente dentro de un negocio (UNIQUE (negocio_id, email)). */
    Optional<Cliente> findByNegocioIdAndEmail(UUID negocioId, String email);
}
