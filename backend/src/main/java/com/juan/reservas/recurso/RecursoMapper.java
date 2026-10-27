package com.juan.reservas.recurso;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.juan.reservas.recurso.dto.RecursoDto;

@Mapper
public interface RecursoMapper {

    @Mapping(target = "servicioIds", expression = "java(recurso.servicioIds())")
    RecursoDto aDto(Recurso recurso);
}
