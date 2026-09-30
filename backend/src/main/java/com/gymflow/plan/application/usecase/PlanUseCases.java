package com.gymflow.plan.application.usecase;

import java.util.List;

import com.gymflow.plan.application.dto.PlanRequest;
import com.gymflow.plan.application.dto.PlanResponse;
import com.gymflow.plan.domain.model.MembershipPlan;
import com.gymflow.plan.domain.port.PlanRepository;
import com.gymflow.shared.domain.exception.NotFoundException;
import com.gymflow.shared.domain.model.Actor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// CRUD simple de planes: un solo servicio en vez de tres casos de uso de pocas líneas.
@Service
@RequiredArgsConstructor
public class PlanUseCases {

    private final PlanRepository plans;

    @Transactional(readOnly = true)
    public List<PlanResponse> list(boolean includeInactive) {
        return plans.findAll(includeInactive).stream().map(PlanResponse::of).toList();
    }

    /** Nombre duplicado → uk_plan_gym_name → 409 en GlobalExceptionHandler. */
    @Transactional
    public PlanResponse create(Actor actor, PlanRequest req) {
        return PlanResponse.of(plans.save(MembershipPlan.create(actor.gymId(), req.name(), req.durationDays(), req.price())));
    }

    @Transactional
    public PlanResponse update(Long id, PlanRequest req) {
        // con bloqueo: dos ediciones simultáneas no se pisan campo a campo
        MembershipPlan plan = plans.lockById(id).orElseThrow(() -> new NotFoundException("Plan no encontrado"));
        boolean active = req.active() == null ? plan.active() : req.active();
        return PlanResponse.of(plans.save(plan.update(req.name(), req.durationDays(), req.price(), active)));
    }
}
