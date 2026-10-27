package com.juan.reservas.servicio;

import org.mapstruct.Mapper;

import com.juan.reservas.servicio.dto.ServicioDto;

@Mapper
public interface ServicioMapper {

    ServicioDto aDto(Servicio servicio);
}
