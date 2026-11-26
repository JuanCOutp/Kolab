package com.kolab.servicio.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kolab.dao.ArchivoDeLog;
import com.kolab.modelo.dto.Historial;
import com.kolab.servicio.HistorialService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Implementación de {@link HistorialService}: convierte cada línea del log en JSON en una fila de la tabla.
 * El archivo es el mismo que se ve en Kudu.
 */
@Service
public class HistorialServiceImpl implements HistorialService {

    // la tabla muestra las ultimas 200 que cumplen el filtro, no el archivo entero
    private static final int MAXIMO = 200;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss")
            .withZone(ZoneId.of("America/Lima"));

    private final ArchivoDeLog archivo;
    private final ObjectMapper json;

    public HistorialServiceImpl(ArchivoDeLog archivo, ObjectMapper json) {
        this.archivo = archivo;
        this.json = json;
    }

    @Override
    public Historial leer(String nivel, String texto) {
        List<String> filas = archivo.lineas();
        String buscado = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        List<Historial.Linea> lineas = new ArrayList<>();
        long errores = 0;
        long avisos = 0;
        long informativos = 0;
        // de abajo hacia arriba: lo ultimo que paso sale primero
        for (int i = filas.size() - 1; i >= 0; i--) {
            Historial.Linea linea = convertir(filas.get(i));
            if (linea == null) {
                continue;
            }
            switch (linea.nivel()) {
                case "ERROR" -> errores++;
                case "WARN" -> avisos++;
                case "INFO" -> informativos++;
                default -> { }
            }
            if (lineas.size() < MAXIMO && cumple(linea, nivel, buscado)) {
                lineas.add(linea);
            }
        }
        return new Historial(lineas, errores, avisos, informativos);
    }

    private Historial.Linea convertir(String fila) {
        try {
            JsonNode nodo = json.readTree(fila);
            String mensaje = nodo.path("message").asText("");
            String error = nodo.path("error").path("message").asText("");
            if (!error.isEmpty()) {
                mensaje = mensaje + " · " + error;
            }
            return new Historial.Linea(hora(nodo.path("@timestamp").asText("")),
                    nodo.path("log").path("level").asText(""),
                    origen(nodo.path("log").path("logger").asText("")), mensaje);
        } catch (JsonProcessingException e) {
            // una linea cortada a la mitad, porque se estaba escribiendo, se salta
            return null;
        }
    }

    private static boolean cumple(Historial.Linea linea, String nivel, String buscado) {
        boolean nivelOk = nivel == null || nivel.isBlank() || nivel.equalsIgnoreCase(linea.nivel());
        boolean textoOk = buscado.isEmpty() || linea.mensaje().toLowerCase(Locale.ROOT).contains(buscado)
                || linea.origen().toLowerCase(Locale.ROOT).contains(buscado);
        return nivelOk && textoOk;
    }

    // la hora viene en utc y la pantalla la muestra en hora de lima
    private static String hora(String marca) {
        try {
            return HORA.format(Instant.parse(marca));
        } catch (DateTimeParseException e) {
            return marca;
        }
    }

    // com.kolab.controlador.ManejoDeErrores -> ManejoDeErrores
    private static String origen(String logger) {
        int punto = logger.lastIndexOf('.');
        return punto < 0 ? logger : logger.substring(punto + 1);
    }
}
