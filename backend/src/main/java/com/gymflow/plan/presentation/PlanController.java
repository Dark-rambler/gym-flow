package com.gymflow.plan.presentation;

import java.util.List;

import com.gymflow.plan.application.dto.PlanRequest;
import com.gymflow.plan.application.dto.PlanResponse;
import com.gymflow.plan.application.usecase.PlanUseCases;
import com.gymflow.shared.infrastructure.security.CurrentActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Plans")
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanUseCases plans;

    @Operation(operationId = "listPlans", summary = "GET /api/plans — planes del gimnasio (activos por defecto)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<PlanResponse> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return plans.list(includeInactive);
    }

    @Operation(operationId = "createPlan", summary = "POST /api/plans — crea un plan")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public PlanResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlanRequest req) {
        return plans.create(CurrentActor.of(jwt), req);
    }

    @Operation(operationId = "updatePlan", summary = "PUT /api/plans/{id} — edita nombre, duración, precio o estado")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public PlanResponse update(@PathVariable Long id, @Valid @RequestBody PlanRequest req) {
        return plans.update(id, req);
    }
}
