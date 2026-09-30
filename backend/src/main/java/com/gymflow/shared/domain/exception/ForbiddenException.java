package com.gymflow.shared.domain.exception;

// El usuario está autenticado y tiene el rol, pero la regla de negocio no le permite la acción → 403.
public class ForbiddenException extends DomainException {

    public ForbiddenException(String message) {
        super(message);
    }
}
