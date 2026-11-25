package com.kolab.servicio;

import com.kolab.modelo.dto.Monitoreo;

/**
 * lee como esta la aplicacion, la base de datos y el servidor en este momento
 */
public interface MonitoreoService {

    Monitoreo leer();
}
