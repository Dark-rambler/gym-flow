package com.gymflow.member.application.dto;

import java.time.LocalDate;

import com.gymflow.member.domain.model.Member;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String fullName,
        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "^\\s*[A-Za-z0-9]{6,12}\\s*$", message = "DNI inválido (6 a 12 letras o números)") String dni,
        @Size(max = 20) @Pattern(regexp = "^[0-9+()\\s-]*$", message = "Teléfono inválido") String phone,
        @Email(message = "Email inválido") @Size(max = 160) String email,
        @Past(message = "La fecha de nacimiento debe ser pasada") LocalDate birthDate,
        @Size(max = 500) String notes) {

    public Member.MemberData toData() {
        return new Member.MemberData(fullName, dni, phone, email, birthDate, notes);
    }
}
