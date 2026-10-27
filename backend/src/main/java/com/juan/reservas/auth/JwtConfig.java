package com.juan.reservas.auth;

import java.nio.charset.StandardCharsets;
import java.time.Clock;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Codificador y decodificador de JWT firmados con HMAC-SHA256 (clave simétrica).
 * <p>
 * Basta con una clave simétrica porque el mismo backend emite y valida los tokens.
 * Si otro servicio tuviera que validarlos sin poder emitirlos, se usaría RSA (clave pública).
 */
@Configuration
public class JwtConfig {

    @Bean
    public SecretKey jwtSecretKey(AuthProperties props) {
        return new SecretKeySpec(props.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    /**
     * Valida firma, emisor y caducidad. La caducidad se comprueba con el Clock de la
     * aplicación, así los tests pueden simular que el token ha expirado.
     */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey, AuthProperties props, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        JwtTimestampValidator caducidad = new JwtTimestampValidator();
        caducidad.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                caducidad, new JwtIssuerValidator(props.jwtIssuer())));
        return decoder;
    }
}
