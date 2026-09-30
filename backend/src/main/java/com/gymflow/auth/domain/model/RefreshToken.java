package com.gymflow.auth.domain.model;

import java.time.Instant;

// Solo se persiste el hash SHA-256 del token; el valor en claro lo tiene únicamente el cliente.
public record RefreshToken(Long id, Long userId, Long gymId, String tokenHash, Instant expiresAt, Instant revokedAt) {

    public static RefreshToken issue(Long userId, Long gymId, String tokenHash, Instant expiresAt) {
        return new RefreshToken(null, userId, gymId, tokenHash, expiresAt, null);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public RefreshToken revoke(Instant now) {
        return new RefreshToken(id, userId, gymId, tokenHash, expiresAt, now);
    }
}
