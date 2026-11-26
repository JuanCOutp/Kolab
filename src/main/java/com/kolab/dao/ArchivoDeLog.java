package com.kolab.dao;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * lee el log en json que escribe logback (logs/kolab.json). lo usa el historial del panel
 */
@Repository
public class ArchivoDeLog {

    private final Path archivo;

    public ArchivoDeLog(@Value("${kolab.archivo-log:logs/kolab.json}") String archivo) {
        this.archivo = Path.of(archivo);
    }

    // logback lo corta en 10 MB y empieza uno nuevo cada dia, asi que leerlo entero no pesa
    public List<String> lineas() {
        if (!Files.exists(archivo)) {
            return List.of();
        }
        try {
            return Files.readAllLines(archivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("no se pudo leer " + archivo, e);
        }
    }
}
