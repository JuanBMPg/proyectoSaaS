package com.juan.reservas.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Configuración de seguridad PROVISIONAL (fase 0).
 * <p>
 * Deja abiertas las rutas públicas y de autenticación y cierra todo lo demás.
 * Como aún no hay filtro JWT, el panel responde siempre 401. El filtro se añade
 * en la fase 1 junto con el registro y el login.
 */
@Configuration
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
        return http
                // API sin estado con JWT en cabecera: no hay cookie de sesión que proteger frente a CSRF
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> resolver.resolveException(req, res, null, e))
                        .accessDeniedHandler((req, res, e) -> resolver.resolveException(req, res, null, e)))
                .build();
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
