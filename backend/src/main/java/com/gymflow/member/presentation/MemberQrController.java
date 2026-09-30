package com.gymflow.member.presentation;

import java.util.UUID;

import com.gymflow.member.application.usecase.MemberQrUseCases;
import com.gymflow.member.application.usecase.MemberQrUseCases.MemberQrResponse;
import com.gymflow.member.application.usecase.MemberQrUseCases.PublicMemberCardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Members")
@RestController
@RequiredArgsConstructor
public class MemberQrController {

    private final MemberQrUseCases qr;

    @Operation(operationId = "getMemberQr", summary = "GET /api/members/{id}/qr — QR del socio (para imprimir el carnet)")
    @GetMapping("/api/members/{id}/qr")
    @PreAuthorize("isAuthenticated()")
    public MemberQrResponse get(@PathVariable Long id) {
        return qr.get(id);
    }

    @Operation(operationId = "rotateMemberQr", summary = "POST /api/members/{id}/qr/rotate — nuevo QR; el carnet y enlace anteriores dejan de valer")
    @PostMapping("/api/members/{id}/qr/rotate")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public MemberQrResponse rotate(@PathVariable Long id) {
        return qr.rotate(id);
    }

    @Operation(operationId = "getPublicMemberCard", summary = "GET /api/public/member-card/{token} — carnet del socio SIN login (enlace para el celular)")
    @GetMapping("/api/public/member-card/{token}")
    public PublicMemberCardResponse publicCard(@PathVariable UUID token) {
        return qr.publicCard(token);
    }
}
