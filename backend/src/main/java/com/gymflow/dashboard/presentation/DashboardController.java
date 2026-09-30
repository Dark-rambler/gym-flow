package com.gymflow.dashboard.presentation;

import com.gymflow.dashboard.application.DashboardUseCases;
import com.gymflow.dashboard.application.DashboardUseCases.DashboardSummaryResponse;
import com.gymflow.shared.infrastructure.security.CurrentActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard")
@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardUseCases dashboard;

    @Operation(operationId = "getDashboardSummary", summary = "GET /api/dashboard/summary — activos, congelados, asistencias de hoy y por vencer")
    @GetMapping("/api/dashboard/summary")
    @PreAuthorize("isAuthenticated()")
    public DashboardSummaryResponse summary(@AuthenticationPrincipal Jwt jwt) {
        return dashboard.summary(CurrentActor.of(jwt));
    }
}
