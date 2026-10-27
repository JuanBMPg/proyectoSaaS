package com.juan.reservas.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.juan.reservas.auth.dto.LoginRequest;
import com.juan.reservas.auth.dto.RegistroRequest;
import com.juan.reservas.comun.ConflictoException;
import com.juan.reservas.comun.NoAutorizadoException;
import com.juan.reservas.negocio.Negocio;
import com.juan.reservas.negocio.NegocioRepository;

/** Registro de negocios, inicio, renovación y cierre de sesión del personal. */
@Service
public class AuthService {

    /** Mismo mensaje para email inexistente y contraseña incorrecta: no revela qué emails existen. */
    static final String CREDENCIALES_INCORRECTAS = "Email o contraseña incorrectos";

    private final NegocioRepository negocioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final Clock clock;

    /**
     * Hash de una contraseña cualquiera. Si el email no existe se compara contra él para
     * que la respuesta tarde lo mismo que con un email real (evita descubrir emails midiendo tiempos).
     */
    private final String hashFicticio;

    public AuthService(NegocioRepository negocioRepository, UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder, JwtService jwtService, RefreshTokenService refreshTokenService,
            Clock clock) {
        this.negocioRepository = negocioRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.clock = clock;
        this.hashFicticio = passwordEncoder.encode("contraseña-ficticia-para-igualar-tiempos");
    }

    /** Crea el negocio y su propietario en una sola transacción y deja la sesión iniciada. */
    @Transactional
    public Sesion registrar(RegistroRequest peticion) {
        String email = normalizarEmail(peticion.email());
        if (negocioRepository.existsBySlug(peticion.slug())) {
            throw new ConflictoException("Ya existe un negocio con ese slug");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictoException("Ya existe un usuario con ese email");
        }

        Instant ahora = Instant.now(clock);
        Negocio negocio = negocioRepository.save(
                new Negocio(peticion.nombreNegocio().trim(), peticion.slug(), ahora));
        Usuario propietario = usuarioRepository.save(new Usuario(
                negocio, email, passwordEncoder.encode(peticion.password()),
                peticion.nombre().trim(), Rol.PROPIETARIO, ahora));

        return iniciarSesion(propietario);
    }

    @Transactional
    public Sesion login(LoginRequest peticion) {
        Usuario usuario = usuarioRepository.findByEmail(normalizarEmail(peticion.email())).orElse(null);

        String hash = usuario != null ? usuario.getPasswordHash() : hashFicticio;
        boolean passwordCorrecta = passwordEncoder.matches(peticion.password(), hash);

        if (usuario == null || !passwordCorrecta || !usuario.isActivo()) {
            throw new NoAutorizadoException(CREDENCIALES_INCORRECTAS);
        }
        return iniciarSesion(usuario);
    }

    /** Rota el refresh token: revoca el recibido y emite un par nuevo. */
    @Transactional
    public Sesion refrescar(String refreshToken) {
        Usuario usuario = refreshTokenService.consumir(refreshToken);
        if (!usuario.isActivo()) {
            throw new NoAutorizadoException("El usuario está desactivado");
        }
        return iniciarSesion(usuario);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revocar(refreshToken);
    }

    private Sesion iniciarSesion(Usuario usuario) {
        return new Sesion(jwtService.emitirAccessToken(usuario), refreshTokenService.emitir(usuario));
    }

    /** "  Juan@Example.com " y "juan@example.com" son el mismo usuario. */
    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
