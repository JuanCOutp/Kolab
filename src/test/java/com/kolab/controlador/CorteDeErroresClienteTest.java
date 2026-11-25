package com.kolab.controlador;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

// ningun campo que manda el navegador entra al log con mas de 300 caracteres
@Tag("unitaria")
class CorteDeErroresClienteTest {

    @Test
    void unTextoLargoSeCortaEn300() {
        String largo = "x".repeat(1000);

        assertThat(ErroresClienteController.cortar(largo)).hasSize(300);
    }

    @Test
    void unTextoCortoQuedaIgualYNadaQuedaVacio() {
        assertThat(ErroresClienteController.cortar("fallo en la linea 12")).isEqualTo("fallo en la linea 12");
        assertThat(ErroresClienteController.cortar(null)).isEmpty();
    }
}
