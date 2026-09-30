package com.gymflow.plan.domain.port;

import java.util.List;
import java.util.Optional;

import com.gymflow.plan.domain.model.MembershipPlan;

/** Consultas filtradas por el gym actual (@TenantId). */
public interface PlanRepository {

    MembershipPlan save(MembershipPlan plan);

    Optional<MembershipPlan> findById(Long id);

    /** findById con SELECT ... FOR UPDATE. Requiere transacción. */
    Optional<MembershipPlan> lockById(Long id);

    List<MembershipPlan> findAll(boolean includeInactive);
}
