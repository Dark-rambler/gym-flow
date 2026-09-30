package com.gymflow.auth.presentation;

import com.gymflow.auth.application.dto.MeResponse;
import com.gymflow.auth.application.usecase.GetCurrentUserUseCase;
import com.gymflow.shared.infrastructure.security.CurrentActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Me")
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final GetCurrentUserUseCase getCurrentUser;

    @Operation(operationId = "getMe", summary = "GET /api/me — usuario autenticado y su gimnasio")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return getCurrentUser.execute(CurrentActor.of(jwt));
    }
}
