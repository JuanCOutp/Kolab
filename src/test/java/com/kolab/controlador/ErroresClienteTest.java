package com.kolab.controlador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// un error de javascript que manda el navegador queda en el log y en el contador
@SpringBootTest
@AutoConfigureMockMvc
@Tag("integracion")
@ExtendWith(OutputCaptureExtension.class)
class ErroresClienteTest {

    private static final String ERROR = """
            {"mensaje": "Cannot read properties of null", "archivo": "/js/vista-previa.js",
             "linea": 12, "pagina": "/mis-solicitudes/nueva"}""";

    @Autowired private MockMvc mockMvc;
    @Autowired private MeterRegistry metricas;

    @Test
    void conTokenQuedaEnElLogYSubeElContador(CapturedOutput salida) throws Exception {
        double antes = metricas.counter("kolab.errores.frontend").count();

        mockMvc.perform(post("/errores-cliente").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(ERROR))
                .andExpect(status().isNoContent());

        assertThat(salida.getOut()).contains("kolab.frontend").contains("Cannot read properties of null");
        assertThat(metricas.counter("kolab.errores.frontend").count()).isEqualTo(antes + 1);
    }

    @Test
    void sinTokenSeRechaza() throws Exception {
        mockMvc.perform(post("/errores-cliente").contentType(MediaType.APPLICATION_JSON).content(ERROR))
                .andExpect(status().isForbidden());
    }
}
