package com.juan.reservas;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Servicios reales en contenedores Docker para los tests de integración: los mismos que
 * levanta el docker-compose en desarrollo.
 * <ul>
 *   <li>PostgreSQL: {@code @ServiceConnection} configura el DataSource con la URL, el usuario
 *       y la contraseña del contenedor, sin tocar application.yml.</li>
 *   <li>Mailpit: servidor de correo de pruebas. Los emails no salen a internet y su API HTTP
 *       permite comprobar qué se ha enviado.</li>
 * </ul>
 * Los contenedores se crean una vez y los comparten todos los tests que usan el mismo contexto.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    public static final int MAILPIT_SMTP = 1025;
    public static final int MAILPIT_API = 8025;

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
    }

    @Bean
    GenericContainer<?> mailpit() {
        return new GenericContainer<>(DockerImageName.parse("axllent/mailpit:latest"))
                .withExposedPorts(MAILPIT_SMTP, MAILPIT_API)
                .waitingFor(Wait.forHttp("/readyz").forPort(MAILPIT_API));
    }

    /** Spring Mail no tiene @ServiceConnection para Mailpit: las propiedades se dan a mano. */
    @Bean
    DynamicPropertyRegistrar propiedadesMailpit(GenericContainer<?> mailpit) {
        return registro -> {
            registro.add("spring.mail.host", mailpit::getHost);
            registro.add("spring.mail.port", () -> mailpit.getMappedPort(MAILPIT_SMTP));
            registro.add("mailpit.api", () -> "http://" + mailpit.getHost() + ":" + mailpit.getMappedPort(MAILPIT_API));
        };
    }
}
