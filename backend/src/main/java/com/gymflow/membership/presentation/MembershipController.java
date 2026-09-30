package com.gymflow.membership.presentation;

import com.gymflow.membership.application.dto.AssignMembershipRequest;
import com.gymflow.membership.application.dto.MembershipResponse;
import com.gymflow.membership.application.usecase.MembershipUseCases;
import com.gymflow.shared.infrastructure.security.CurrentActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Memberships")
@RestController
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipUseCases useCases;

    @Operation(operationId = "assignMembership",
            summary = "POST /api/members/{memberId}/memberships — vende o renueva (encadenada al último vencimiento)")
    @PostMapping("/api/members/{memberId}/memberships")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public MembershipResponse assign(@AuthenticationPrincipal Jwt jwt, @PathVariable Long memberId,
                                     @Valid @RequestBody AssignMembershipRequest req) {
        return useCases.assign(CurrentActor.of(jwt), memberId, req);
    }

    @Operation(operationId = "freezeMembership", summary = "POST /api/memberships/{id}/freeze — congela desde hoy")
    @PostMapping("/api/memberships/{id}/freeze")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public MembershipResponse freeze(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return useCases.freeze(CurrentActor.of(jwt), id);
    }

    @Operation(operationId = "unfreezeMembership",
            summary = "POST /api/memberships/{id}/unfreeze — descongela y suma los días congelados al vencimiento")
    @PostMapping("/api/memberships/{id}/unfreeze")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public MembershipResponse unfreeze(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return useCases.unfreeze(CurrentActor.of(jwt), id);
    }

    @Operation(operationId = "cancelMembership", summary = "POST /api/memberships/{id}/cancel — cancela la membresía")
    @PostMapping("/api/memberships/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public MembershipResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return useCases.cancel(CurrentActor.of(jwt), id);
    }
}
