package com.gymflow.auth.application.usecase;

import java.time.Instant;

import com.gymflow.auth.application.dto.AuthResponse;
import com.gymflow.auth.application.dto.MeResponse;
import com.gymflow.auth.domain.model.AppUser;
import com.gymflow.auth.domain.model.RefreshToken;
import com.gymflow.auth.domain.model.TokenHashes;
import com.gymflow.auth.domain.port.RefreshTokenRepository;
import com.gymflow.auth.domain.port.TokenIssuer;
import com.gymflow.gym.domain.model.Gym;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Emite access token + refresh token nuevo. Debe llamarse dentro de la transacción del caso de uso.
@Component
@RequiredArgsConstructor
class SessionIssuer {

    private final TokenIssuer tokenIssuer;
    private final RefreshTokenRepository refreshTokens;

    AuthResponse start(AppUser user, Gym gym) {
        var access = tokenIssuer.issueAccessToken(user);
        String refresh = TokenHashes.newOpaqueToken();
        Instant expiresAt = Instant.now().plus(tokenIssuer.refreshTokenTtl());
        refreshTokens.save(RefreshToken.issue(user.id(), user.gymId(), TokenHashes.sha256Hex(refresh), expiresAt));
        return new AuthResponse(access.value(), refresh, access.expiresInSeconds(), MeResponse.of(user, gym));
    }
}
