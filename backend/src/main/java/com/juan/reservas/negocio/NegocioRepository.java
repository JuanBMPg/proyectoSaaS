package com.juan.reservas.negocio;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NegocioRepository extends JpaRepository<Negocio, UUID> {

    boolean existsBySlug(String slug);

    Optional<Negocio> findBySlug(String slug);
}
