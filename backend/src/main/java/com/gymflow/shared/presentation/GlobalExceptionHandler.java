package com.gymflow.shared.presentation;

import java.util.LinkedHashMap;
import java.util.Map;

import com.gymflow.shared.domain.exception.ConflictException;
import com.gymflow.shared.domain.exception.ForbiddenException;
import com.gymflow.shared.domain.exception.NotFoundException;
import com.gymflow.shared.domain.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return respond(HttpStatus.BAD_REQUEST, "Datos inválidos", errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException ex) {
        return respond(HttpStatus.BAD_REQUEST, "Cuerpo de la petición inválido", null);
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(NotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> noResource(NoResourceFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, "Recurso no encontrado", null);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ErrorResponse> conflict(ConflictException ex) {
        return respond(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ErrorResponse> unauthorized(UnauthorizedException ex) {
        return respond(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    ResponseEntity<ErrorResponse> forbidden(RuntimeException ex) {
        String message = ex instanceof ForbiddenException ? ex.getMessage() : "No tienes permiso para esta acción";
        return respond(HttpStatus.FORBIDDEN, message, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException ex) {
        String detail = String.valueOf(ex.getMostSpecificCause().getMessage());
        String message = detail.contains("uk_app_user_email")
                ? "El email ya está registrado"
                : "El registro entra en conflicto con datos existentes";
        return respond(HttpStatus.CONFLICT, message, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception ex) {
        log.error("Error no controlado", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", null);
    }

    private static ResponseEntity<ErrorResponse> respond(HttpStatus status, String message, Map<String, String> errors) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message, errors));
    }
}
