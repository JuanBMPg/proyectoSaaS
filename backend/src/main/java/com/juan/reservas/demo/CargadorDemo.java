package com.juan.reservas.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Carga el negocio de demostración al arrancar, solo si {@code app.demo.habilitada=true}
 * (variable de entorno DEMO). Así la demo pública y el docker-compose arrancan con datos,
 * y una instalación normal arranca vacía.
 */
@Component
@ConditionalOnProperty(name = "app.demo.habilitada", havingValue = "true")
public class CargadorDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CargadorDemo.class);

    private final DatosDemo datos;

    public CargadorDemo(DatosDemo datos) {
        this.datos = datos;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        if (datos.cargar()) {
            log.info("Datos de demostración creados: /{} (propietaria {} / {})",
                    DatosDemo.SLUG, DatosDemo.EMAIL_PROPIETARIA, DatosDemo.PASSWORD);
        } else {
            log.info("Los datos de demostración ya existían");
        }
    }
}
