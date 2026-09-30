package com.gymflow.plan.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

// Plan que vende el gimnasio (p. ej. "Mensual", 30 días, S/ 100).
public record MembershipPlan(
        Long id,
        Long gymId,
        String name,
        int durationDays,
        BigDecimal price,
        boolean active,
        Instant createdAt) {

    public static MembershipPlan create(Long gymId, String name, int durationDays, BigDecimal price) {
        return new MembershipPlan(null, gymId, name.trim(), durationDays, price, true, null);
    }

    public MembershipPlan update(String newName, int newDurationDays, BigDecimal newPrice, boolean newActive) {
        return new MembershipPlan(id, gymId, newName.trim(), newDurationDays, newPrice, newActive, createdAt);
    }
}
