package com.gymflow.gym.domain.model;

import java.time.Instant;

// Tenant raíz: cada gimnasio registrado en la plataforma.
public record Gym(Long id, String name, String slug, String timezone, String currency, Instant createdAt) {

    public static final String DEFAULT_TIMEZONE = "America/Lima";
    public static final String DEFAULT_CURRENCY = "PEN";

    public static Gym create(String name, String slug) {
        return new Gym(null, name.trim(), slug, DEFAULT_TIMEZONE, DEFAULT_CURRENCY, null);
    }
}
