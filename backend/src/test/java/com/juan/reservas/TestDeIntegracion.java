package com.juan.reservas;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Configuración común de los tests de integración: contexto completo de Spring,
 * PostgreSQL en Testcontainers, MockMvc y el perfil "test".
 * Si no hay Docker disponible, el test se marca como omitido (en el CI siempre hay Docker).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public @interface TestDeIntegracion {
}
