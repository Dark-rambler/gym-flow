package com.gymflow.auth.application.usecase;

import java.time.Instant;
import java.util.Optional;

import com.gymflow.auth.application.dto.AuthResponse;
import com.gymflow.auth.domain.model.RefreshToken;
import com.gymflow.auth.domain.model.TokenHashes;
import com.gymflow.auth.domain.port.RefreshTokenRepository;
import com.gymflow.auth.domain.port.UserRepository;
import com.gymflow.gym.domain.port.GymRepository;
import com.gymflow.shared.domain.exception.UnauthorizedException;
import com.gymflow.shared.infrastructure.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Rota el refresh token: lo consume de forma atómica y emite uno nuevo.
 * Si el token ya estaba usado/revocado (reutilización → posible robo), revoca todas las sesiones del usuario.
 * El front serializa los refresh entre pestañas (Web Locks) para no disparar esto en uso normal.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshSessionUseCase {

    private static final String INVALID = "Sesión expirada, vuelve a iniciar sesión";

    private final RefreshTokenRepository refreshTokens;
    private final UserRepository users;
    private final GymRepository gyms;
    private final SessionIssuer sessions;
    private final TransactionTemplate tx;

    public AuthResponse execute(String rawToken) {
        // La transacción hace commit también en caso de reutilización (para persistir la revocación);
        // el 401 se lanza fuera.
        Optional<AuthResponse> result = TenantContext.callAsSystem(() -> tx.execute(status -> {
            Instant now = Instant.now();
            RefreshToken token = refreshTokens.findByHash(TokenHashes.sha256Hex(rawToken)).orElse(null);
            if (token == null || token.isExpired(now)) {
                return Optional.<AuthResponse>empty();
            }
            if (token.isRevoked() || !refreshTokens.consume(token.id(), now)) {
                log.warn("Reutilización de refresh token: se revocan todas las sesiones (userId={}, gymId={})",
                        token.userId(), token.gymId());
                refreshTokens.revokeAllActiveForUser(token.userId(), now);
                return Optional.<AuthResponse>empty();
            }
            var user = users.findById(token.userId()).filter(u -> u.active()).orElse(null);
            var gym = user == null ? null : gyms.findById(user.gymId()).orElse(null);
            if (gym == null) {
                return Optional.<AuthResponse>empty();
            }
            return Optional.of(sessions.start(user, gym));
        }));
        return result.orElseThrow(() -> new UnauthorizedException(INVALID));
    }
}
