package com.juan.reservas.auth;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Emite los access tokens (JWT) del personal de los negocios. */
@Service
public class JwtService {

    public static final String CLAIM_NEGOCIO_ID = "negocioId";
    public static final String CLAIM_ROL = "rol";

    private final JwtEncoder encoder;
    private final AuthProperties props;
    private final Clock clock;

    public JwtService(JwtEncoder encoder, AuthProperties props, Clock clock) {
        this.encoder = encoder;
        this.props = props;
        this.clock = clock;
    }

    /**
     * El token lleva todo lo necesario para autorizar sin consultar la base de datos:
     * quién es (sub), de qué negocio (negocioId) y con qué rol.
     */
    public TokenEmitido emitirAccessToken(Usuario usuario) {
        Instant ahora = Instant.now(clock);
        Instant expiraEn = ahora.plus(props.accessTokenTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.jwtIssuer())
                .subject(usuario.getId().toString())
                .issuedAt(ahora)
                .expiresAt(expiraEn)
                .claim(CLAIM_NEGOCIO_ID, usuario.getNegocio().getId().toString())
                .claim(CLAIM_ROL, usuario.getRol().name())
                .build();

        // Hay que indicar HS256: por defecto el encoder espera una clave RSA (RS256)
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
        return new TokenEmitido(token, expiraEn);
    }
}
