package com.kolab.servicio.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kolab.dao.ArchivoDeLog;
import com.kolab.modelo.dto.Historial;
import com.kolab.servicio.HistorialService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Tag("unitaria")
class HistorialServiceImplTest {

    private final ArchivoDeLog archivo = Mockito.mock(ArchivoDeLog.class);
    private final HistorialService historial = new HistorialServiceImpl(archivo, new ObjectMapper());

    @BeforeEach
    void unLogDeEjemplo() {
        // tres lineas como las escribe logback en formato ecs, y una cortada a la mitad
        Mockito.when(archivo.lineas()).thenReturn(List.of(
                linea("2026-11-26T14:00:00Z", "INFO", "com.kolab.seguridad", "ingreso correcto · cuenta=pa***@gmail.com"),
                linea("2026-11-26T14:05:00Z", "WARN", "kolab.seguridad", "ingreso fallido · cuenta=wa***@utp.edu.pe"),
                linea("2026-11-26T14:10:00Z", "ERROR", "com.kolab.controlador.ManejoDeErrores", "error inesperado en /servicios/5"),
                "{\"@timestamp\":\"2026-11-26T14:11"));
    }

    @Test
    void loUltimoSaleArribaYLaHoraEsDeLima() {
        Historial h = historial.leer(null, null);

        assertThat(h.lineas()).extracting(Historial.Linea::nivel).containsExactly("ERROR", "WARN", "INFO");
        // 14:10 en utc son las 09:10 en lima
        assertThat(h.lineas().get(0).hora()).isEqualTo("26/11 09:10:00");
        assertThat(h.lineas().get(0).origen()).isEqualTo("ManejoDeErrores");
    }

    @Test
    void cuentaCuantasHayDeCadaNivel() {
        Historial h = historial.leer(null, null);

        assertThat(h.errores()).isEqualTo(1);
        assertThat(h.avisos()).isEqualTo(1);
        assertThat(h.informativos()).isEqualTo(1);
    }

    @Test
    void filtraPorNivel() {
        assertThat(historial.leer("WARN", null).lineas())
                .singleElement().extracting(Historial.Linea::mensaje).asString().contains("ingreso fallido");
    }

    @Test
    void buscaPorTextoSinImportarMayusculas() {
        assertThat(historial.leer(null, "INGRESO").lineas()).hasSize(2);
        assertThat(historial.leer(null, "servicios/5").lineas()).hasSize(1);
    }

    @Test
    void sinArchivoNoHayLineas() {
        Mockito.when(archivo.lineas()).thenReturn(List.of());

        assertThat(historial.leer(null, null).lineas()).isEmpty();
    }

    private static String linea(String hora, String nivel, String logger, String mensaje) {
        return "{\"@timestamp\":\"" + hora + "\",\"log\":{\"level\":\"" + nivel + "\",\"logger\":\"" + logger
                + "\"},\"process\":{\"pid\":38},\"message\":\"" + mensaje + "\"}";
    }
}
