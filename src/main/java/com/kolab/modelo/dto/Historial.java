package com.kolab.modelo.dto;

import java.util.List;

/**
 * lo que muestra la pantalla de historial: las ultimas lineas del log y cuantas hay de cada nivel
 */
public record Historial(List<Linea> lineas, long errores, long avisos, long informativos) {

    // una linea del log ya lista para la tabla
    public record Linea(String hora, String nivel, String origen, String mensaje) {
    }
}
