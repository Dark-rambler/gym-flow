package com.gymflow.auth.domain.model;

import java.time.Instant;
import java.util.Locale;

import com.gymflow.shared.domain.model.Role;

// Usuario del staff de un gimnasio. El email es único en toda la plataforma (el login es solo por email).
public record AppUser(
        Long id,
        Long gymId,
        String email,
        String passwordHash,
        String fullName,
        Role role,
        boolean active,
        Instant createdAt) {

    public static AppUser create(Long gymId, String email, String passwordHash, String fullName, Role role) {
        return new AppUser(null, gymId, normalizeEmail(email), passwordHash, fullName.trim(), role, true, null);
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public AppUser withRole(Role newRole) {
        return new AppUser(id, gymId, email, passwordHash, fullName, newRole, active, createdAt);
    }

    public AppUser withActive(boolean newActive) {
        return new AppUser(id, gymId, email, passwordHash, fullName, role, newActive, createdAt);
    }
}
