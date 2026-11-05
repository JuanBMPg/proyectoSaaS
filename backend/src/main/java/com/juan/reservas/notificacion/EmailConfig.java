package com.juan.reservas.notificacion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Motor de plantillas solo para los emails. La aplicación es una API REST y no renderiza
 * páginas, así que no se usa la integración de Thymeleaf con Spring MVC.
 * <p>
 * Cada email tiene dos plantillas en resources/templates/email: la HTML y la de texto plano
 * (para los programas de correo que no muestran HTML).
 */
@Configuration
public class EmailConfig {

    @Bean
    public TemplateEngine motorPlantillasEmail() {
        TemplateEngine motor = new TemplateEngine();
        motor.addTemplateResolver(resolutor(".html", TemplateMode.HTML, 1));
        motor.addTemplateResolver(resolutor(".txt", TemplateMode.TEXT, 2));
        return motor;
    }

    /** checkExistence: si no hay plantilla con esa extensión, prueba con el siguiente resolutor. */
    private static ClassLoaderTemplateResolver resolutor(String extension, TemplateMode modo, int orden) {
        ClassLoaderTemplateResolver resolutor = new ClassLoaderTemplateResolver();
        resolutor.setPrefix("templates/email/");
        resolutor.setSuffix(extension);
        resolutor.setTemplateMode(modo);
        resolutor.setCharacterEncoding("UTF-8");
        resolutor.setCheckExistence(true);
        resolutor.setOrder(orden);
        return resolutor;
    }
}
