package com.juan.reservas.recurso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.juan.reservas.comun.NoEncontradoException;
import com.juan.reservas.negocio.Negocio;
import com.juan.reservas.negocio.NegocioRepository;
import com.juan.reservas.recurso.dto.AsignacionServiciosRequest;
import com.juan.reservas.servicio.Servicio;
import com.juan.reservas.servicio.ServicioRepository;

@ExtendWith(MockitoExtension.class)
class RecursoServiceTest {

    @Mock
    private RecursoRepository repository;
    @Mock
    private ServicioRepository servicioRepository;
    @Mock
    private NegocioRepository negocioRepository;

    private RecursoService service;
    private Negocio negocio;
    private Recurso marta;
    private Servicio corte;

    @BeforeEach
    void setUp() {
        // El mapper real generado por MapStruct: no tiene sentido simularlo
        service = new RecursoService(repository, servicioRepository, negocioRepository,
                Mappers.getMapper(RecursoMapper.class));
        negocio = new Negocio("Peluquería Lola", "lola", Instant.parse("2026-06-02T08:00:00Z"));
        marta = new Recurso(negocio, "Marta", TipoRecurso.PERSONA);
        corte = new Servicio(negocio, "Corte", null, 30, 0, 1500);
    }

    @Test
    void unRecursoDeOtroNegocioSeTrataComoInexistente() {
        UUID otroNegocio = UUID.randomUUID();
        when(repository.findByIdAndNegocioId(marta.getId(), otroNegocio)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desactivar(otroNegocio, marta.getId()))
                .isInstanceOf(NoEncontradoException.class);
        assertThat(marta.isActivo()).isTrue();
    }

    @Test
    void asignaLosServiciosDelNegocio() {
        when(repository.findByIdAndNegocioId(marta.getId(), negocio.getId())).thenReturn(Optional.of(marta));
        when(servicioRepository.findAllByIdInAndNegocioId(any(), eq(negocio.getId()))).thenReturn(List.of(corte));

        var dto = service.asignarServicios(negocio.getId(), marta.getId(),
                new AsignacionServiciosRequest(Set.of(corte.getId())));

        assertThat(dto.servicioIds()).containsExactly(corte.getId());
    }

    @Test
    void siAlgunServicioNoEsDelNegocioDevuelve404YNoCambiaNada() {
        UUID servicioAjeno = UUID.randomUUID();
        when(repository.findByIdAndNegocioId(marta.getId(), negocio.getId())).thenReturn(Optional.of(marta));
        // La consulta filtrada por negocio solo encuentra uno de los dos
        when(servicioRepository.findAllByIdInAndNegocioId(any(), eq(negocio.getId()))).thenReturn(List.of(corte));

        assertThatThrownBy(() -> service.asignarServicios(negocio.getId(), marta.getId(),
                new AsignacionServiciosRequest(Set.of(corte.getId(), servicioAjeno))))
                .isInstanceOf(NoEncontradoException.class)
                .hasMessage("Servicio no encontrado");
        assertThat(marta.servicioIds()).isEmpty();
    }
}
