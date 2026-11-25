package com.kolab.controlador;

import com.kolab.modelo.dto.ErrorDeCliente;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// recibe los errores de javascript que pasan en el navegador de la gente, que si no nadie se entera
@Controller
public class ErroresClienteController {

    // logger aparte para separar los errores del navegador de los del servidor
    private static final Logger log = LoggerFactory.getLogger("kolab.frontend");
    static final int MAXIMO = 300;

    private final MeterRegistry metricas;

    public ErroresClienteController(MeterRegistry metricas) {
        this.metricas = metricas;
    }

    @PostMapping("/errores-cliente")
    public ResponseEntity<Void> recibir(@RequestBody ErrorDeCliente error) {
        log.warn("error en el navegador · mensaje={} · archivo={} · linea={} · pagina={}",
                cortar(error.mensaje()), cortar(error.archivo()), error.linea(), cortar(error.pagina()));
        metricas.counter("kolab.errores.frontend").increment();
        return ResponseEntity.noContent().build();
    }

    // lo manda cualquiera, asi que ningun campo entra entero al log
    static String cortar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.length() > MAXIMO ? texto.substring(0, MAXIMO) : texto;
    }
}
