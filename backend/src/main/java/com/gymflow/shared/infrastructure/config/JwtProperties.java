package com.gymflow.shared.infrastructure.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, Duration accessTtl, Duration refreshTtl) {

    public static final String ISSUER = "gymflow";

    public JwtProperties {
        if (secret == null || secret.isBlank() || secret.startsWith("${")) {
            throw new IllegalStateException("Falta JWT_SECRET");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes");
        }
    }
}
