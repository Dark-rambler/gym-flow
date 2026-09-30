package com.gymflow.membership.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** price opcional: si viene, reemplaza el precio del plan (solo OWNER/ADMIN). La fecha de inicio la calcula el backend. */
public record AssignMembershipRequest(
        @NotNull(message = "El plan es obligatorio") Long planId,
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Precio inválido") BigDecimal price) {
}
