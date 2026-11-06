package com.juan.reservas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa las tareas {@code @Scheduled}: el envío de la cola de emails y los recordatorios.
 * Spring Boot las ejecuta en su pool de tareas programadas ("taskScheduler").
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
