package com.gymflow.auth.infrastructure.security;

import java.time.Duration;
import java.time.Instant;

import com.gymflow.auth.domain.model.AccessToken;
import com.gymflow.auth.domain.model.AppUser;
import com.gymflow.auth.domain.port.TokenIssuer;
import com.gymflow.shared.infrastructure.config.JwtProperties;
import com.gymflow.shared.infrastructure.tenant.GymTenantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class JwtTokenIssuer implements TokenIssuer {

    private final JwtEncoder encoder;
    private final JwtProperties props;

    @Override
    public AccessToken issueAccessToken(AppUser user) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer(JwtProperties.ISSUER)
                .subject(user.id().toString())
                .issuedAt(now)
                .expiresAt(now.plus(props.accessTtl()))
                .claim(GymTenantResolver.GYM_ID_CLAIM, user.gymId())
                .claim("role", user.role().name())
                .claim("name", user.fullName())
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, props.accessTtl().toSeconds());
    }

    @Override
    public Duration refreshTokenTtl() {
        return props.refreshTtl();
    }
}
