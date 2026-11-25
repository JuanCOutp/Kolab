package com.kolab.modelo.dto;

/**
 * lo que muestra la pantalla de monitoreo del panel: la aplicacion, la base y el servidor en un momento
 */
public record Monitoreo(String hora, Aplicacion aplicacion, BaseDeDatos baseDeDatos, Sistema sistema) {

    // cuantas peticiones atendio, que tan rapido y cuantas salieron mal
    public record Aplicacion(long peticiones, long tiempoMedioMs, long tiempoMaximoMs,
                             long respuestas2xx, long respuestas4xx, long respuestas5xx,
                             long erroresBackend, long erroresFrontend,
                             long memoriaUsadaMb, long memoriaMaximaMb, int porcentajeMemoria,
                             String encendido) {
    }

    // tamano es un texto porque en H2 no se puede saber
    public record BaseDeDatos(String estado, int conexionesActivas, int conexionesInactivas,
                              int conexionesMaximas, int porcentajeConexiones, String tamano) {
    }

    // red es un texto porque solo se puede leer en el servidor linux
    public record Sistema(int cpuSistema, int cpuProceso, long memoriaTotalMb, long memoriaLibreMb,
                          int porcentajeMemoria, long discoTotalGb, long discoLibreGb, int porcentajeDisco,
                          String red) {
    }
}
