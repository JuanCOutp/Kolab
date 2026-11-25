package com.kolab.controlador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// un error que nadie esperaba: la persona ve la pagina generica y el error queda en el log y en el contador
@SpringBootTest
@AutoConfigureMockMvc
@Import(ErrorInesperadoTest.RutaQueFalla.class)
@Tag("integracion")
@ExtendWith(OutputCaptureExtension.class)
class ErrorInesperadoTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MeterRegistry metricas;

    @Test
    void muestraLaPaginaGenericaYLoRegistraConSuTraza(CapturedOutput salida) throws Exception {
        double antes = metricas.counter("kolab.errores.backend").count();

        mockMvc.perform(get("/prueba/falla").with(user("cliente@kolab.pe").roles("USUARIO")))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString("Algo salió mal")))
                .andExpect(content().string(not(containsString("se rompio a proposito"))));

        assertThat(salida.getOut()).contains("ERROR").contains("error inesperado en /prueba/falla")
                .contains("IllegalStateException: se rompio a proposito");
        assertThat(metricas.counter("kolab.errores.backend").count()).isEqualTo(antes + 1);
    }

    // solo existe en esta prueba: una ruta que siempre falla
    @RestController
    static class RutaQueFalla {

        @GetMapping("/prueba/falla")
        String fallar() {
            throw new IllegalStateException("se rompio a proposito");
        }
    }
}
