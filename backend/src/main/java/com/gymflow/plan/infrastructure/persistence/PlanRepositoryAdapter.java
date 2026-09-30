package com.gymflow.plan.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.gymflow.plan.domain.model.MembershipPlan;
import com.gymflow.plan.domain.port.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class PlanRepositoryAdapter implements PlanRepository {

    private final PlanJpaRepository jpa;

    @Override
    public MembershipPlan save(MembershipPlan plan) {
        var entity = new PlanJpaEntity();
        entity.setId(plan.id());
        entity.setGymId(plan.gymId());
        entity.setName(plan.name());
        entity.setDurationDays(plan.durationDays());
        entity.setPrice(plan.price());
        entity.setActive(plan.active());
        entity.setCreatedAt(plan.createdAt());
        // flush inmediato: uk_plan_gym_name sale aquí y no al commit
        return toDomain(jpa.saveAndFlush(entity));
    }

    @Override
    public Optional<MembershipPlan> findById(Long id) {
        return jpa.findScopedById(id).map(PlanRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<MembershipPlan> lockById(Long id) {
        return jpa.lockScopedById(id).map(PlanRepositoryAdapter::toDomain);
    }

    @Override
    public List<MembershipPlan> findAll(boolean includeInactive) {
        var plans = includeInactive
                ? jpa.findAllByOrderByDurationDaysAscNameAsc()
                : jpa.findByActiveTrueOrderByDurationDaysAscNameAsc();
        return plans.stream().map(PlanRepositoryAdapter::toDomain).toList();
    }

    private static MembershipPlan toDomain(PlanJpaEntity e) {
        return new MembershipPlan(e.getId(), e.getGymId(), e.getName(), e.getDurationDays(), e.getPrice(),
                e.isActive(), e.getCreatedAt());
    }
}
