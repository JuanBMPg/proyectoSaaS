package com.juan.reservas.negocio;

import org.mapstruct.Mapper;

/** MapStruct genera la implementación en tiempo de compilación (target/generated-sources). */
@Mapper
public interface NegocioMapper {

    NegocioDto aDto(Negocio negocio);
}
