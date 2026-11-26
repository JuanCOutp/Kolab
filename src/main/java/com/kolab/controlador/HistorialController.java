package com.kolab.controlador;

import com.kolab.servicio.HistorialService;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

// va en /admin, asi que la cadena del panel en SecurityConfig solo deja entrar al administrador
@Controller
public class HistorialController {

    private static final Set<String> NIVELES = Set.of("ERROR", "WARN", "INFO");

    private final HistorialService historialService;

    public HistorialController(HistorialService historialService) {
        this.historialService = historialService;
    }

    @GetMapping("/admin/historial")
    public String historial(@RequestParam(required = false) String nivel,
                            @RequestParam(required = false) String buscar, Model model) {
        String elegido = nivel != null && NIVELES.contains(nivel) ? nivel : null;
        model.addAttribute("seccion", "historial");
        model.addAttribute("nivel", elegido);
        model.addAttribute("buscar", buscar);
        model.addAttribute("historial", historialService.leer(elegido, buscar));
        return "administracion/historial";
    }
}
