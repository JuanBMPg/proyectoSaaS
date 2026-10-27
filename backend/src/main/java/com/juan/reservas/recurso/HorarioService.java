package com.juan.reservas.recurso;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.comun.NoEncontradoException;
import com.juan.reservas.recurso.dto.FranjaHorariaRequest;
import com.juan.reservas.recurso.dto.HorarioDto;
import com.juan.reservas.recurso.dto.HorarioRequest;

/** Horario semanal de los recursos. Siempre se comprueba antes que el recurso es del negocio. */
@Service
public class HorarioService {

    private final HorarioRepository repository;
    private final RecursoRepository recursoRepository;
    private final HorarioMapper mapper;

    public HorarioService(HorarioRepository repository, RecursoRepository recursoRepository,
            HorarioMapper mapper) {
        this.repository = repository;
        this.recursoRepository = recursoRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public HorarioDto obtener(UUID negocioId, UUID recursoId) {
        comprobarRecurso(negocioId, recursoId);
        return aDto(repository.findAllByRecursoIdOrderByDiaSemanaAscHoraInicioAsc(recursoId));
    }

    /** Sustituye la semana completa: borra las franjas actuales e inserta las nuevas. */
    @Transactional
    public HorarioDto reemplazar(UUID negocioId, UUID recursoId, HorarioRequest peticion) {
        comprobarRecurso(negocioId, recursoId);
        // getReferenceById no consulta la BD: basta con la referencia para rellenar recurso_id
        Recurso recurso = recursoRepository.getReferenceById(recursoId);

        repository.borrarDeRecurso(recursoId);
        List<Horario> nuevas = repository.saveAll(peticion.franjas().stream()
                .map(franja -> nuevaFranja(recurso, franja))
                .toList());

        return aDto(nuevas.stream()
                .sorted(Comparator.comparing(Horario::getDiaSemana).thenComparing(Horario::getHoraInicio))
                .toList());
    }

    private void comprobarRecurso(UUID negocioId, UUID recursoId) {
        if (!recursoRepository.existsByIdAndNegocioId(recursoId, negocioId)) {
            throw new NoEncontradoException("Recurso no encontrado");
        }
    }

    private static Horario nuevaFranja(Recurso recurso, FranjaHorariaRequest franja) {
        return new Horario(recurso, DayOfWeek.of(franja.diaSemana()), franja.horaInicio(), franja.horaFin());
    }

    private HorarioDto aDto(List<Horario> franjas) {
        return new HorarioDto(franjas.stream().map(mapper::aDto).toList());
    }
}
