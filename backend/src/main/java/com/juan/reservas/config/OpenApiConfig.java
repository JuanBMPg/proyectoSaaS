package com.juan.reservas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Metadatos de la documentación OpenAPI (Swagger UI en /swagger-ui.html).
 * Declara el esquema "bearer" para poder probar desde el navegador las rutas que piden JWT.
 */
@Configuration
public class OpenApiConfig {

    public static final String ESQUEMA_JWT = "bearer-jwt";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Reservas API")
                        .description("API de la plataforma de reservas para pequeños negocios")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
