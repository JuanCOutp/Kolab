package com.kolab.servicio;

import com.kolab.modelo.dto.Historial;

/**
 * Lee el log en JSON que escribe la aplicación para mostrarlo en el panel del administrador.
 */
public interface HistorialService {

    /**
     * Devuelve las últimas líneas del log, las más nuevas primero, filtradas por nivel y por texto.
     * Los dos filtros son opcionales.
     */
    Historial leer(String nivel, String texto);
}
