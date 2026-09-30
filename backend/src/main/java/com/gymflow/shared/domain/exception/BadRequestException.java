package com.gymflow.shared.domain.exception;

// Petición válida en forma pero con valores que no tienen sentido (p. ej. rango de fechas invertido) → 400.
public class BadRequestException extends DomainException {

    public BadRequestException(String message) {
        super(message);
    }
}
