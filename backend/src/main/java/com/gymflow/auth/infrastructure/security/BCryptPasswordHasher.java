package com.gymflow.auth.infrastructure.security;

import java.nio.charset.StandardCharsets;

import com.gymflow.auth.domain.port.PasswordHasher;
import com.gymflow.shared.application.validation.ValidPassword;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class BCryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder;

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        // BCrypt rechaza > 72 bytes con excepción (→ 500); ninguna contraseña guardada puede superarlo.
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > ValidPassword.MAX_BYTES) {
            return false;
        }
        return encoder.matches(rawPassword, hash);
    }
}
