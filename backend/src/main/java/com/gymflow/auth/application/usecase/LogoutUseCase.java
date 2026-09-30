package com.gymflow.auth.application.usecase;

import java.time.Instant;

import com.gymflow.auth.domain.model.TokenHashes;
import com.gymflow.auth.domain.port.RefreshTokenRepository;
import com.gymflow.shared.infrastructure.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

// Revoca el refresh token si existe. Idempotente: nunca falla para no filtrar si un token es válido.
@Service
@RequiredArgsConstructor
public class LogoutUseCase {

    private final RefreshTokenRepository refreshTokens;
    private final TransactionTemplate tx;

    public void execute(String rawToken) {
        TenantContext.callAsSystem(() -> tx.execute(status -> {
            refreshTokens.findByHash(TokenHashes.sha256Hex(rawToken))
                    .filter(t -> !t.isRevoked())
                    .ifPresent(t -> refreshTokens.save(t.revoke(Instant.now())));
            return null;
        }));
    }
}
