package com.juan.reservas;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL real en un contenedor Docker para los tests de integración.
 * <p>
 * {@code @ServiceConnection} hace que Spring Boot configure el DataSource con la URL,
 * el usuario y la contraseña del contenedor, sin tocar application.yml.
 * Se usa la misma imagen que en docker-compose para probar contra la misma versión.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
    }
}
