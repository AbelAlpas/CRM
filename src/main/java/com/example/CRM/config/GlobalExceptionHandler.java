package com.example.CRM.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.security.access.AccessDeniedException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public String handleUnexpectedException(Exception exception, HttpServletRequest request, Model model) {
        log.error("Unexpected error while processing {}", request.getRequestURI(), exception);
        model.addAttribute("pageTitle", "Error");
        model.addAttribute("activeModule", "");
        model.addAttribute("message", "No pudimos completar la solicitud. Intentalo nuevamente.");
        return "error/general";
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException exception, HttpServletRequest request, Model model) {
        log.warn("Access denied for {}", request.getRequestURI());
        model.addAttribute("pageTitle", "Acceso denegado");
        model.addAttribute("activeModule", "");
        model.addAttribute("message", "No tienes permisos para acceder a este recurso.");
        return "error/general";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(IllegalArgumentException exception, HttpServletRequest request, Model model) {
        log.warn("Invalid CRM resource request at {}: {}", request.getRequestURI(), exception.getMessage());
        model.addAttribute("pageTitle", "Recurso no encontrado");
        model.addAttribute("activeModule", "");
        model.addAttribute("message", exception.getMessage());
        return "error/general";
    }
}
