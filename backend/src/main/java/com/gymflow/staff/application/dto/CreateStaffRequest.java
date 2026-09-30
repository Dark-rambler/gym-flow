package com.gymflow.staff.application.dto;

import com.gymflow.shared.domain.model.Role;
import com.gymflow.shared.application.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateStaffRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String fullName,
        @NotBlank(message = "El email es obligatorio") @Email(message = "Email inválido") @Size(max = 160) String email,
        @NotNull @ValidPassword String password,
        @NotNull(message = "El rol es obligatorio") Role role) {
}
