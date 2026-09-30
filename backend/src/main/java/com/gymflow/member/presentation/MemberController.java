package com.gymflow.member.presentation;

import com.gymflow.member.application.dto.MemberDetailResponse;
import com.gymflow.member.application.dto.MemberRequest;
import com.gymflow.member.application.dto.MemberSummaryResponse;
import com.gymflow.member.application.usecase.MemberUseCases;
import com.gymflow.shared.application.dto.PageResponse;
import com.gymflow.shared.infrastructure.security.CurrentActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Members")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberUseCases members;

    public record SetActiveRequest(@NotNull Boolean active) {
    }

    @Operation(operationId = "searchMembers", summary = "GET /api/members — busca por nombre o DNI (paginado, page desde 0)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<MemberSummaryResponse> search(@AuthenticationPrincipal Jwt jwt,
                                                      @RequestParam(required = false) String q,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return members.search(CurrentActor.of(jwt), q, page, size);
    }

    @Operation(operationId = "createMember", summary = "POST /api/members — registra un socio")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public MemberDetailResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MemberRequest req) {
        return members.create(CurrentActor.of(jwt), req);
    }

    @Operation(operationId = "getMember", summary = "GET /api/members/{id} — ficha del socio con historial de membresías")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public MemberDetailResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return members.get(CurrentActor.of(jwt), id);
    }

    @Operation(operationId = "updateMember", summary = "PUT /api/members/{id} — edita los datos del socio")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public MemberDetailResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                       @Valid @RequestBody MemberRequest req) {
        return members.update(CurrentActor.of(jwt), id, req);
    }

    @Operation(operationId = "setMemberActive", summary = "PATCH /api/members/{id}/active — activa o desactiva al socio")
    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public MemberDetailResponse setActive(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                          @Valid @RequestBody SetActiveRequest req) {
        return members.setActive(CurrentActor.of(jwt), id, req.active());
    }
}
