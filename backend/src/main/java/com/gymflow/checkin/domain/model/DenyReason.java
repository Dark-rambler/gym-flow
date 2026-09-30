package com.gymflow.checkin.domain.model;

/** Por qué se rechazó una entrada. El mensaje se muestra tal cual en recepción. */
public enum DenyReason {
    INVALID_CODE("Código no reconocido"),
    UNKNOWN("No se encontró ningún socio con ese QR o DNI"),
    MEMBER_INACTIVE("El socio está desactivado"),
    NO_MEMBERSHIP("No tiene membresía"),
    NOT_STARTED("Su membresía aún no empieza"),
    FROZEN("Su membresía está congelada"),
    EXPIRED("Su membresía venció");

    private final String message;

    DenyReason(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
