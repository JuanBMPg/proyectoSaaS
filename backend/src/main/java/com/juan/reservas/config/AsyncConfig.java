package com.juan.reservas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Activa {@code @Async}: los métodos anotados se ejecutan en el pool de hilos que Spring Boot
 * configura por defecto ("applicationTaskExecutor"). Se usa para enviar emails sin hacer
 * esperar al cliente que espera la respuesta HTTP.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
