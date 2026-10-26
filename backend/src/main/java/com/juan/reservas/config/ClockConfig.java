package com.juan.reservas.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj único de la aplicación. Los servicios piden la hora con {@code Instant.now(clock)}
 * en lugar de {@code Instant.now()}, así los tests pueden fijar "ahora" con {@code Clock.fixed(...)}.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
