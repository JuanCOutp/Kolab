package com.kolab.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;

// un correo con salto de linea no puede inventar una linea nueva en el log de seguridad
@SpringBootTest
@AutoConfigureMockMvc
@Tag("seguridad")
@ExtendWith(OutputCaptureExtension.class)
class BitacoraSinLineasFalsasTest {

    private static final String LINEA_FALSA = "ingreso correcto · cuenta=admin@kolab.pe";

    @Autowired private MockMvc mockMvc;

    @Test
    void unCorreoConSaltoDeLineaNoInventaUnaLineaEnElLog(CapturedOutput salida) throws Exception {
        String correoTrucado = "x@kolab.pe\n" + LINEA_FALSA;

        mockMvc.perform(formLogin("/login").userParameter("email").user(correoTrucado).password("mala"));

        assertThat(salida.getOut()).contains("ingreso fallido");
        assertThat(salida.getOut()).doesNotContain("\n" + LINEA_FALSA);
    }
}
