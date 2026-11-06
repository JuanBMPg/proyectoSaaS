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

    /**
     * Los cambios de reglas se aplican a lo que se reserve a partir de ahora. Las reservas
     * existentes no cambian: si se activa la confirmación manual, las ya confirmadas siguen
     * confirmadas.
     */
    @Transactional
    public NegocioDto actualizar(UUID negocioId, NegocioRequest peticion) {
        Negocio negocio = repository.findById(negocioId)
                .orElseThrow(() -> new NoEncontradoException("Negocio no encontrado"));
        negocio.actualizar(peticion.nombre().trim(), opcional(peticion.email()), opcional(peticion.telefono()),
                opcional(peticion.direccion()), peticion.etiquetaRecurso().trim(), peticion.antelacionMinimaMin(),
                peticion.antelacionMaximaDias(), peticion.plazoCancelacionHoras(),
                peticion.confirmacionAutomatica());
        return mapper.aDto(negocio);
    }

    private static String opcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
