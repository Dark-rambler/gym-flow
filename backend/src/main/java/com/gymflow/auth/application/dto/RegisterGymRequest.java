package com.gymflow.auth.application.dto;

import com.gymflow.shared.application.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterGymRequest(
        @NotBlank(message = "El nombre del gimnasio es obligatorio") @Size(max = 120) String gymName,
        @NotBlank(message = "Tu nombre es obligatorio") @Size(max = 120) String ownerName,
        @NotBlank(message = "El email es obligatorio") @Email(message = "Email inválido") @Size(max = 160) String email,
        @NotNull @ValidPassword String password) {
}
