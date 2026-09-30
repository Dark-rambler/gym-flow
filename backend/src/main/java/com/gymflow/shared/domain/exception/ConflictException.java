package com.gymflow.shared.domain.exception;

// Conflicto con el estado actual (duplicados, reglas de negocio violadas) → 409.
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
