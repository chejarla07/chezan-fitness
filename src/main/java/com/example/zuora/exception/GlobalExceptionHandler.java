package com.example.zuora.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFound(ResourceNotFoundException e, Model model) {
        logger.warn("Resource not found: {}", e.getMessage());
        model.addAttribute("error", e.getMessage());
        return "error/404";
    }

    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(BusinessException e, RedirectAttributes redirectAttributes) {
        logger.warn("Business error: {}", e.getMessage());
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/";
    }

    @ExceptionHandler(ZuoraApiException.class)
    public String handleZuoraApiException(ZuoraApiException e, RedirectAttributes redirectAttributes) {
        logger.error("Zuora API error: {}", e.getMessage(), e);
        redirectAttributes.addFlashAttribute("error", "A billing system error occurred. Please try again later.");
        return "redirect:/";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException e) {
        logger.warn("Access denied: {}", e.getMessage());
        return "redirect:/login?error=unauthorized";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        logger.error("Unexpected error: {}", e.getMessage(), e);
        model.addAttribute("error", "An unexpected error occurred. Please try again later.");
        return "error/500";
    }
}