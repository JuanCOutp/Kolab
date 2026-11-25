package com.kolab.controlador;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.kolab.modelo.dto.Monitoreo;
import com.kolab.modelo.entidad.EstadoUsuario;
import com.kolab.modelo.entidad.Rol;
import com.kolab.modelo.entidad.Usuario;
import com.kolab.seguridad.BitacoraDeSeguridad;
import com.kolab.seguridad.DetallesUsuarioService;
import com.kolab.seguridad.SecurityConfig;
import com.kolab.seguridad.UsuarioAutenticado;
import com.kolab.servicio.CatalogoService;
import com.kolab.servicio.MensajeService;
import com.kolab.servicio.MonitoreoService;
import com.kolab.servicio.OfertaService;
import com.kolab.servicio.SolicitudService;
import com.kolab.util.Fotos;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// la pantalla de monitoreo solo la ve el administrador
@WebMvcTest(MonitoreoController.class)
@Import(SecurityConfig.class)
@Tag("controlador")
class MonitoreoControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MonitoreoService monitoreoService;
    @MockitoBean private DetallesUsuarioService detallesUsuarioService;
    @MockitoBean private BitacoraDeSeguridad bitacora;
    @MockitoBean private Fotos fotos;
    @MockitoBean private CatalogoService catalogoService;
    @MockitoBean private MensajeService mensajeService;
    @MockitoBean private SolicitudService solicitudService;
    @MockitoBean private OfertaService ofertaService;

    @Test
    void elAdministradorVeLosTresBloques() throws Exception {
        Mockito.when(monitoreoService.leer()).thenReturn(unaLectura());

        mockMvc.perform(get("/admin/monitoreo").with(user(sesion(Rol.ADMINISTRADOR))))
                .andExpect(status().isOk())
                .andExpect(view().name("administracion/monitoreo"))
                .andExpect(content().string(containsString("id=\"bloque-aplicacion\"")))
                .andExpect(content().string(containsString("id=\"bloque-base\"")))
                .andExpect(content().string(containsString("id=\"bloque-sistema\"")))
                .andExpect(content().string(containsString("1520")));
    }

    @Test
    void aUnUsuarioLeRespondeComoSiNoExistiera() throws Exception {
        mockMvc.perform(get("/admin/monitoreo").with(user(sesion(Rol.USUARIO))))
                .andExpect(status().isNotFound());

        Mockito.verifyNoInteractions(monitoreoService);
    }

    @Test
    void sinSesionMandaAlIngresoDelPanel() throws Exception {
        mockMvc.perform(get("/admin/monitoreo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/admin/login"));
    }

    private static Monitoreo unaLectura() {
        return new Monitoreo("24/11/2026 20:15:03",
                new Monitoreo.Aplicacion(1520, 35, 410, 1400, 110, 0, 0, 0, 120, 256, 47, "2 h 13 min"),
                new Monitoreo.BaseDeDatos("UP", 1, 9, 10, 10, "12 MB"),
                new Monitoreo.Sistema(12, 3, 1024, 324, 68, 30, 22, 27, "solo en el servidor Linux"));
    }

    private static UsuarioAutenticado sesion(Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Equipo");
        usuario.setApellidos("KOLAB");
        usuario.setEmail("admin@kolab.pe");
        usuario.setPasswordHash("$2a$10$hashfalso");
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setRol(rol);
        return new UsuarioAutenticado(usuario, false, null);
    }
}
