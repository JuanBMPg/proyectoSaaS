package com.juan.reservas.empleado;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.auth.Rol;
import com.juan.reservas.auth.Usuario;
import com.juan.reservas.auth.UsuarioRepository;
import com.juan.reservas.comun.ConflictoException;
import com.juan.reservas.comun.NoEncontradoException;
import com.juan.reservas.empleado.dto.EmpleadoDto;
import com.juan.reservas.empleado.dto.EmpleadoRequest;
import com.juan.reservas.negocio.NegocioRepository;
import com.juan.reservas.recurso.Recurso;
import com.juan.reservas.recurso.RecursoRepository;

/**
 * Empleados del negocio: usuarios con rol EMPLEADO vinculados a un recurso, cuya agenda ven.
 * El propietario les da de alta con una contraseña inicial.
 */
@Service
public class EmpleadoService {

    private final UsuarioRepository usuarioRepository;
    private final RecursoRepository recursoRepository;
    private final NegocioRepository negocioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public EmpleadoService(UsuarioRepository usuarioRepository, RecursoRepository recursoRepository,
            NegocioRepository negocioRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.recursoRepository = recursoRepository;
        this.negocioRepository = negocioRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<EmpleadoDto> listar(UUID negocioId) {
        Map<UUID, Recurso> recursos = recursoRepository.findAllByNegocioIdOrderByNombre(negocioId).stream()
                .collect(Collectors.toMap(Recurso::getId, Function.identity()));
        return usuarioRepository.findAllByNegocioIdAndRolOrderByNombre(negocioId, Rol.EMPLEADO).stream()
                .map(usuario -> aDto(usuario, recursos.get(usuario.getRecursoId())))
                .toList();
    }

    @Transactional
    public EmpleadoDto crear(UUID negocioId, EmpleadoRequest peticion) {
        String email = peticion.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictoException("Ya existe un usuario con ese email");
        }
        // El recurso tiene que ser de este negocio: el esquema no lo garantiza en usuario.recurso_id
        Recurso recurso = recursoRepository.findByIdAndNegocioId(peticion.recursoId(), negocioId)
                .orElseThrow(() -> new NoEncontradoException("Recurso no encontrado"));
        if (usuarioRepository.existsByRecursoId(recurso.getId())) {
            throw new ConflictoException("Ese recurso ya tiene un empleado asignado");
        }

        Usuario empleado = new Usuario(negocioRepository.getReferenceById(negocioId), email,
                passwordEncoder.encode(peticion.password()), peticion.nombre().trim(), Rol.EMPLEADO,
                Instant.now(clock));
        empleado.asignarRecurso(recurso.getId());
        return aDto(usuarioRepository.save(empleado), recurso);
    }

    private static EmpleadoDto aDto(Usuario usuario, Recurso recurso) {
        EmpleadoDto.Recurso recursoDto = recurso == null ? null : new EmpleadoDto.Recurso(recurso.getId(), recurso.getNombre());
        return new EmpleadoDto(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.isActivo(), recursoDto);
    }
}
