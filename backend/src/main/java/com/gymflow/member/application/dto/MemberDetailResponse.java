package com.gymflow.member.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.gymflow.member.domain.model.Member;
import com.gymflow.membership.application.dto.MembershipResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ficha del socio: datos, membresía actual e historial (más reciente primero).
 * El qrToken NO se incluye: es una credencial de acceso (check-in) y tendrá su propio endpoint con rotación.
 */
public record MemberDetailResponse(
        Long id,
        String fullName,
        String dni,
        @Schema(nullable = true) String phone,
        @Schema(nullable = true) String email,
        @Schema(nullable = true) LocalDate birthDate,
        @Schema(nullable = true) String notes,
        boolean active,
        Instant createdAt,
        @Schema(nullable = true) MembershipResponse currentMembership,
        List<MembershipResponse> memberships) {

    public static MemberDetailResponse of(Member m, MembershipResponse current, List<MembershipResponse> history) {
        return new MemberDetailResponse(m.id(), m.fullName(), m.dni(), m.phone(), m.email(), m.birthDate(), m.notes(),
                m.active(), m.createdAt(), current, history);
    }
}
