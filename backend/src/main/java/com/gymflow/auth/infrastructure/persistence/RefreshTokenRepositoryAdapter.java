package com.gymflow.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;

import com.gymflow.auth.domain.model.RefreshToken;
import com.gymflow.auth.domain.port.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;

    @Override
    public RefreshToken save(RefreshToken token) {
        var entity = token.id() == null ? new RefreshTokenJpaEntity() : jpa.getReferenceById(token.id());
        entity.setUserId(token.userId());
        entity.setGymId(token.gymId());
        entity.setTokenHash(token.tokenHash());
        entity.setExpiresAt(token.expiresAt());
        entity.setRevokedAt(token.revokedAt());
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<RefreshToken> findByHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(RefreshTokenRepositoryAdapter::toDomain);
    }

    @Override
    public boolean consume(Long id, Instant now) {
        return jpa.consume(id, now) == 1;
    }

    @Override
    public void revokeAllActiveForUser(Long userId, Instant now) {
        jpa.revokeAllActiveForUser(userId, now);
    }

    private static RefreshToken toDomain(RefreshTokenJpaEntity e) {
        return new RefreshToken(e.getId(), e.getUserId(), e.getGymId(), e.getTokenHash(), e.getExpiresAt(), e.getRevokedAt());
    }
}
