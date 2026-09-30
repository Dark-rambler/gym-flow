package com.gymflow.plan.application.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Alta y edición. En el alta, active se ignora (un plan nuevo siempre nace activo).
public record PlanRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String name,
        @NotNull(message = "La duración es obligatoria") @Min(value = 1, message = "Mínimo 1 día")
        @Max(value = 730, message = "Máximo 730 días") Integer durationDays,
        @NotNull(message = "El precio es obligatorio") @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @DecimalMax(value = "99999.99", message = "Precio demasiado alto")
        @Digits(integer = 5, fraction = 2, message = "Precio inválido") BigDecimal price,
        Boolean active) {
}
