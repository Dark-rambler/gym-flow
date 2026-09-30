package com.gymflow.shared.domain.exception;

// Base de las excepciones de negocio; GlobalExceptionHandler las traduce a HTTP.
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
