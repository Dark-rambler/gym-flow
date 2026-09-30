package com.gymflow.shared.presentation;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

// Cuerpo de error único de la API: { status, message, timestamp, errors? }.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int status, String message, Instant timestamp, Map<String, String> errors) {

    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(status.value(), message, Instant.now(), null);
    }

    public static ErrorResponse of(HttpStatus status, String message, Map<String, String> errors) {
        return new ErrorResponse(status.value(), message, Instant.now(), errors);
    }
}
