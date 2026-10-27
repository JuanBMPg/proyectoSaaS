package com.juan.reservas.negocio;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.comun.NoEncontradoException;

@Service
public class NegocioService {

    private final NegocioRepository repository;
    private final NegocioMapper mapper;

    public NegocioService(NegocioRepository repository, NegocioMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public NegocioDto obtener(UUID negocioId) {
        return repository.findById(negocioId)
                .map(mapper::aDto)
                .orElseThrow(() -> new NoEncontradoException("Negocio no encontrado"));
    }
}
