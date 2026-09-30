package com.gymflow.auth.domain.port;

import java.time.Instant;
import java.util.Optional;

import com.gymflow.auth.domain.model.RefreshToken;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByHash(String tokenHash);

    /** Marca el token como usado solo si seguía vigente; false si otra petición ya lo consumió o fue revocado. */
    boolean consume(Long id, Instant now);

    void revokeAllActiveForUser(Long userId, Instant now);
}
