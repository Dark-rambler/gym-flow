package com.gymflow.shared.infrastructure.security;

import com.gymflow.shared.domain.exception.UnauthorizedException;
import com.gymflow.shared.domain.model.Actor;
import com.gymflow.shared.domain.model.Role;
import com.gymflow.shared.infrastructure.tenant.GymTenantResolver;
import com.gymflow.shared.infrastructure.tenant.TenantContext;
import org.springframework.security.oauth2.jwt.Jwt;

// Construye el Actor desde el JWT validado. Uso en controllers: @AuthenticationPrincipal Jwt jwt → CurrentActor.of(jwt).
public final class CurrentActor {

    private CurrentActor() {
    }

    public static Actor of(Jwt jwt) {
        long gymId = GymTenantResolver.gymIdFromClaim(jwt.getClaim(GymTenantResolver.GYM_ID_CLAIM));
        try {
            if (gymId == TenantContext.NONE) {
                throw new IllegalArgumentException("gymId");
            }
            return new Actor(Long.valueOf(jwt.getSubject()), gymId, Role.valueOf(jwt.getClaimAsString("role")));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new UnauthorizedException("Token con datos inválidos");
        }
    }
}
