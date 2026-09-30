package com.gymflow.membership.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.gymflow.cash.domain.model.PaymentMethod;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Venta = membresía + cobro completo en la caja abierta.
 * price opcional: reemplaza el precio del plan (solo OWNER/ADMIN). La fecha de inicio la calcula el backend.
 * idempotencyKey: UUID generado por el cliente por cada intento de venta; repetirlo devuelve la venta original.
 */
public record AssignMembershipRequest(
        @NotNull(message = "El plan es obligatorio") Long planId,
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @DecimalMax(value = "99999.99", message = "Precio demasiado alto")
        @Digits(integer = 5, fraction = 2, message = "Precio inválido") BigDecimal price,
        @NotNull(message = "El método de pago es obligatorio") PaymentMethod paymentMethod,
        @Size(max = 40, message = "Máximo 40 caracteres") String paymentReference,
        @NotNull(message = "Falta la clave de idempotencia") UUID idempotencyKey) {
}
