package com.kolab.modelo.dto;

// lo que manda js/errores.js cuando algo falla en el navegador
public record ErrorDeCliente(String mensaje, String archivo, Integer linea, String pagina) {
}
