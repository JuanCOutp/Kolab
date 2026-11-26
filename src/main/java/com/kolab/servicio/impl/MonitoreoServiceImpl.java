package com.kolab.servicio.impl;

import com.kolab.dao.EstadoDeLaBase;
import com.kolab.modelo.dto.Monitoreo;
import com.kolab.servicio.MonitoreoService;
import com.kolab.util.RedDelServidor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

/**
 * arma la pantalla de monitoreo con lo que ya mide spring boot (actuator y micrometer) y con lo que
 * java sabe del servidor. no usa nada externo: el servidor tiene 1 GB de memoria
 */
@Service
public class MonitoreoServiceImpl implements MonitoreoService {

    // el servidor de azure esta en utc, la pantalla muestra la hora de lima
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.of("America/Lima"));
    private static final long MB = 1024L * 1024;
    private static final long GB = MB * 1024;

    private final MeterRegistry metricas;
    private final EstadoDeLaBase base;

    public MonitoreoServiceImpl(MeterRegistry metricas, EstadoDeLaBase base) {
        this.metricas = metricas;
        this.base = base;
    }

    @Override
    public Monitoreo leer() {
        return new Monitoreo(HORA.format(Instant.now()), aplicacion(), baseDeDatos(), sistema());
    }

    // apm: lo que micrometer va contando de cada peticion, mas los dos contadores de errores propios
    private Monitoreo.Aplicacion aplicacion() {
        long peticiones = 0;
        double tiempoTotalMs = 0;
        double tiempoMaximoMs = 0;
        long[] porTipo = new long[6];
        // spring boot guarda un timer por cada combinacion de ruta, metodo y codigo de respuesta
        for (Timer timer : metricas.find("http.server.requests").timers()) {
            peticiones += timer.count();
            tiempoTotalMs += timer.totalTime(TimeUnit.MILLISECONDS);
            tiempoMaximoMs = Math.max(tiempoMaximoMs, timer.max(TimeUnit.MILLISECONDS));
            String codigo = timer.getId().getTag("status");
            if (codigo != null && !codigo.isEmpty() && Character.isDigit(codigo.charAt(0))) {
                porTipo[codigo.charAt(0) - '0'] += timer.count();
            }
        }
        long usada = (long) sumar("jvm.memory.used") / MB;
        long maxima = (long) sumar("jvm.memory.max") / MB;
        return new Monitoreo.Aplicacion(peticiones, peticiones == 0 ? 0 : Math.round(tiempoTotalMs / peticiones),
                Math.round(tiempoMaximoMs), porTipo[2], porTipo[4], porTipo[5],
                contador("kolab.errores.backend"), contador("kolab.errores.frontend"),
                usada, maxima, porcentaje(usada, maxima), encendido());
    }

    private Monitoreo.BaseDeDatos baseDeDatos() {
        int activas = (int) gauge("hikaricp.connections.active");
        int inactivas = (int) gauge("hikaricp.connections.idle");
        int maximas = (int) gauge("hikaricp.connections.max");
        String tamano = base.tamanoEnBytes().map(bytes -> (bytes / MB) + " MB").orElse("no disponible");
        return new Monitoreo.BaseDeDatos(base.responde() ? "UP" : "DOWN", activas, inactivas, maximas,
                porcentaje(activas, maximas), tamano);
    }

    private Monitoreo.Sistema sistema() {
        // la clase de com.sun trae la memoria del servidor; la de java.lang no
        com.sun.management.OperatingSystemMXBean so =
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long memoriaTotal = so.getTotalMemorySize() / MB;
        long memoriaLibre = so.getFreeMemorySize() / MB;
        File raiz = new File("/");
        long discoTotal = raiz.getTotalSpace() / GB;
        long discoLibre = raiz.getUsableSpace() / GB;
        String red = RedDelServidor.leer()
                .map(bytes -> "recibidos " + bytes[0] / MB + " MB · enviados " + bytes[1] / MB + " MB")
                .orElse("solo en el servidor Linux");
        return new Monitoreo.Sistema(enPorcentaje(gauge("system.cpu.usage")), enPorcentaje(gauge("process.cpu.usage")),
                memoriaTotal, memoriaLibre, porcentaje(memoriaTotal - memoriaLibre, memoriaTotal),
                discoTotal, discoLibre, porcentaje(discoTotal - discoLibre, discoTotal), red);
    }

    private String encendido() {
        TimeGauge uptime = metricas.find("process.uptime").timeGauge();
        long minutos = uptime == null ? 0 : (long) uptime.value(TimeUnit.MINUTES);
        return (minutos / 60) + " h " + (minutos % 60) + " min";
    }

    // la memoria viene repartida en varias areas (heap, metaspace...): se suman las del heap
    private double sumar(String nombre) {
        return metricas.find(nombre).tag("area", "heap").gauges().stream()
                .mapToDouble(Gauge::value).filter(valor -> valor > 0).sum();
    }

    private double gauge(String nombre) {
        Gauge gauge = metricas.find(nombre).gauge();
        return gauge == null || Double.isNaN(gauge.value()) ? 0 : gauge.value();
    }

    private long contador(String nombre) {
        Counter contador = metricas.find(nombre).counter();
        return contador == null ? 0 : (long) contador.count();
    }

    // la cpu llega como fraccion: 0.25 es 25 %
    private static int enPorcentaje(double fraccion) {
        return (int) Math.round(Math.max(0, fraccion) * 100);
    }

    private static int porcentaje(long parte, long total) {
        return total <= 0 ? 0 : (int) Math.min(100, Math.round(parte * 100.0 / total));
    }
}
