package com.kolab.controlador;

import com.kolab.servicio.MonitoreoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// va en /admin, asi que la cadena del panel en SecurityConfig solo deja entrar al administrador
@Controller
public class MonitoreoController {

    private final MonitoreoService monitoreoService;

    public MonitoreoController(MonitoreoService monitoreoService) {
        this.monitoreoService = monitoreoService;
    }

    @GetMapping("/admin/monitoreo")
    public String monitoreo(Model model) {
        model.addAttribute("seccion", "monitoreo");
        model.addAttribute("monitoreo", monitoreoService.leer());
        return "administracion/monitoreo";
    }
}
