package com.gymflow.shared.presentation;

import java.util.LinkedHashMap;
import java.util.Map;

import com.gymflow.shared.domain.exception.BadRequestException;
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
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ErrorResponse> badRequest(BadRequestException ex) {
        return respond(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> missingParam(MissingServletRequestParameterException ex) {
        return respond(HttpStatus.BAD_REQUEST, "Falta el parámetro: " + ex.getParameterName(), null);
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

    // constraint de BD → mensaje para el usuario (los nombres vienen de las migraciones Flyway)
    private static final Map<String, String> CONSTRAINT_MESSAGES = Map.of(
            "uk_app_user_email", "El email ya está registrado",
            "uk_member_gym_dni", "Ya existe un socio con ese DNI",
            "uk_plan_gym_name", "Ya existe un plan con ese nombre",
            "uk_cash_session_open", "Ya hay una caja abierta",
            "uk_payment_idempotency", "Esta venta ya fue registrada");

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException ex) {
        String detail = String.valueOf(ex.getMostSpecificCause().getMessage());
        String message = CONSTRAINT_MESSAGES.entrySet().stream()
                .filter(e -> detail.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("El registro entra en conflicto con datos existentes");
        return respond(HttpStatus.CONFLICT, message, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException ex) {
        return respond(HttpStatus.BAD_REQUEST, "Parámetro inválido: " + ex.getName(), null);
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
