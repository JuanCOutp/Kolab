package com.kolab.controlador;

import com.kolab.seguridad.BitacoraDeSeguridad;
import com.kolab.servicio.excepcion.OperacionNoPermitidaException;
import com.kolab.servicio.excepcion.RecursoNoEncontradoException;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class ManejoDeErrores {

    private static final Logger log = LoggerFactory.getLogger("com.kolab");

    private final BitacoraDeSeguridad bitacora;
    // en algunas pruebas no hay registro de metricas: ahi simplemente no se cuenta
    private final ObjectProvider<MeterRegistry> metricas;

    public ManejoDeErrores(BitacoraDeSeguridad bitacora, ObjectProvider<MeterRegistry> metricas) {
        this.bitacora = bitacora;
        this.metricas = metricas;
    }

    // la persona vuelve a la pantalla desde la que lo intentó y ve por qué no se pudo
    @ExceptionHandler(OperacionNoPermitidaException.class)
    public String noPermitida(OperacionNoPermitidaException e, HttpServletRequest pedido,
                              RedirectAttributes flash) {
        flash.addFlashAttribute("error", e.getMessage());
        return "redirect:" + deDondeVino(pedido);
    }

    // un archivo de mas de 10 MB ni llega al controlador: se vuelve al formulario con el aviso
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String archivoEnorme(HttpServletRequest pedido, RedirectAttributes flash) {
        flash.addFlashAttribute("error", "La foto pesa más de 2 MB.");
        return "redirect:" + deDondeVino(pedido);
    }

    /**
     * Pedir algo ajeno y pedir algo que no existe dan la misma respuesta, así que la diferencia
     * solo se ve en la bitácora: varios de estos seguidos son alguien probando ids.
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noEncontrado(HttpServletRequest pedido, Principal quien, Model model) {
        bitacora.noEsSuyo(pedido.getRequestURI(), quien == null ? null : quien.getName(),
                pedido.getRemoteAddr());
        model.addAttribute("status", HttpStatus.NOT_FOUND.value());
        return "error";
    }

    // lo que nadie esperaba: queda en el log con su traza y en el contador de errores, y la persona solo ve
    // la pagina generica, sin detalles de adentro
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String inesperado(Exception e, HttpServletRequest pedido, Model model) throws Exception {
        if (loRespondeSpring(e)) {
            throw e;
        }
        log.error("error inesperado en {}", pedido.getRequestURI(), e);
        metricas.ifAvailable(registro -> registro.counter("kolab.errores.backend").increment());
        model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return "error";
    }

    // 404 de una ruta, 405, un parametro mal escrito o falta de permiso: spring ya sabe responderlos y siguen igual
    private static boolean loRespondeSpring(Exception e) {
        return e instanceof ErrorResponse || e instanceof TypeMismatchException
                || e instanceof HttpMessageNotReadableException || e instanceof BindException
                || e instanceof AccessDeniedException || e instanceof AuthenticationException
                || AnnotatedElementUtils.hasAnnotation(e.getClass(), ResponseStatus.class);
    }

    // solo se vuelve a una ruta de la propia aplicacion, nunca a otro sitio
    private String deDondeVino(HttpServletRequest pedido) {
        String anterior = pedido.getHeader("Referer");
        String base = pedido.getScheme() + "://" + pedido.getServerName();
        if (anterior == null || !anterior.startsWith(base)) {
            return "/";
        }
        int inicio = anterior.indexOf('/', base.length());
        return inicio < 0 ? "/" : anterior.substring(inicio);
    }
}
