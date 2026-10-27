package com.juan.reservas.recurso;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.juan.reservas.recurso.dto.FranjaHorariaDto;

@Mapper
public interface HorarioMapper {

    @Mapping(target = "diaSemana", expression = "java(horario.getDiaSemana().getValue())")
    FranjaHorariaDto aDto(Horario horario);
}
