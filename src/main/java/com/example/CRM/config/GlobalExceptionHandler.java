package com.example.CRM.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

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
}
