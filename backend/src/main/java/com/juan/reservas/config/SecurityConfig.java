package com.juan.reservas.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.juan.reservas.auth.JwtService;

/**
 * Seguridad de la API.
 * <ul>
 *   <li>Rutas públicas y de autenticación abiertas; el resto exige un JWT válido.</li>
 *   <li>El JWT lo valida el resource server de Spring Security (firma, emisor y caducidad).</li>
 *   <li>{@code @EnableMethodSecurity} activa {@code @PreAuthorize} para exigir roles por endpoint.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
            "/api/auth/**",
            "/api/publico/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/error"
    };

    /**
     * Los errores de seguridad (401/403) ocurren en los filtros, antes de llegar a los
     * controladores. Se delegan al {@link HandlerExceptionResolver} para que los formatee
     * el mismo manejador global que el resto de errores (Problem Details).
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {
        AuthenticationEntryPoint noAutenticado = (req, res, e) -> resolver.resolveException(req, res, null, e);

        return http
                // El access token va en la cabecera Authorization, que el navegador no añade solo.
                // La única cookie (refresh token) es SameSite=Strict y solo viaja a /api/auth.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(noAutenticado))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(noAutenticado)
                        .accessDeniedHandler((req, res, e) -> resolver.resolveException(req, res, null, e)))
                .build();
    }

    /** Convierte el claim "rol" del JWT en la autoridad ROLE_PROPIETARIO o ROLE_EMPLEADO. */
    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(JwtService.CLAIM_ROL);
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    /**
     * Codificador "delegante": guarda el algoritmo junto al hash ({bcrypt}$2a$...),
     * así se podría migrar a otro algoritmo en el futuro sin invalidar las contraseñas actuales.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(props.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
