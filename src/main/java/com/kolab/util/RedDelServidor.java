package com.kolab.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * bytes recibidos y enviados por la red desde que prendio el servidor. linux los deja en /proc/net/dev;
 * en windows ese archivo no existe y no hay dato
 */
public final class RedDelServidor {

    private static final Path ARCHIVO = Path.of("/proc/net/dev");

    private RedDelServidor() {
    }

    public static Optional<long[]> leer() {
        if (!Files.isReadable(ARCHIVO)) {
            return Optional.empty();
        }
        try {
            return Optional.of(sumar(Files.readAllLines(ARCHIVO)));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    // cada linea es una tarjeta de red: "eth0: recibidos ... enviados ...". se salta lo (la red interna)
    static long[] sumar(List<String> lineas) {
        long recibidos = 0;
        long enviados = 0;
        for (String linea : lineas) {
            int dosPuntos = linea.indexOf(':');
            if (dosPuntos < 0 || linea.substring(0, dosPuntos).trim().equals("lo")) {
                continue;
            }
            String[] numeros = linea.substring(dosPuntos + 1).trim().split("\\s+");
            // la columna 0 son los bytes recibidos y la 8 los enviados
            recibidos += Long.parseLong(numeros[0]);
            enviados += Long.parseLong(numeros[8]);
        }
        return new long[] {recibidos, enviados};
    }
}
