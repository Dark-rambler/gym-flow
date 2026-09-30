package com.gymflow.shared.infrastructure.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;

/**
 * Resuelve el gym actual para Hibernate @TenantId: override de TenantContext → claim gymId del JWT → NONE.
 * Nunca devuelve SYSTEM por defecto: una ruta pública que toque entidades con tenant no ve nada.
 */
@Component
public class GymTenantResolver implements CurrentTenantIdentifierResolver<Long>, HibernatePropertiesCustomizer {

    public static final String GYM_ID_CLAIM = "gymId";

    @Override
    public Long resolveCurrentTenantIdentifier() {
        Long override = TenantContext.override();
        if (override != null) {
            return override;
        }
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwt) {
            return gymIdFromClaim(jwt.getToken().getClaim(GYM_ID_CLAIM));
        }
        return TenantContext.NONE;
    }

    /** Solo ids positivos: SYSTEM (root) nunca puede venir de un token, únicamente de TenantContext. */
    public static long gymIdFromClaim(Object claim) {
        return claim instanceof Number n && n.longValue() > 0 ? n.longValue() : TenantContext.NONE;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public boolean isRoot(Long tenantId) {
        return tenantId != null && tenantId == TenantContext.SYSTEM;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
