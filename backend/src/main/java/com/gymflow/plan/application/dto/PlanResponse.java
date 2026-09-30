package com.gymflow.plan.application.dto;

import java.math.BigDecimal;

import com.gymflow.plan.domain.model.MembershipPlan;

public record PlanResponse(Long id, String name, int durationDays, BigDecimal price, boolean active) {

    public static PlanResponse of(MembershipPlan p) {
        return new PlanResponse(p.id(), p.name(), p.durationDays(), p.price(), p.active());
    }
}
