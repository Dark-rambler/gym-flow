package com.gymflow.member.application.dto;

import com.gymflow.member.domain.model.Member;
import com.gymflow.membership.application.dto.MembershipResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** Fila del listado de socios con su membresía actual (null si nunca tuvo una). */
public record MemberSummaryResponse(
        Long id,
        String fullName,
        String dni,
        @Schema(nullable = true) String phone,
        boolean active,
        @Schema(nullable = true) MembershipResponse currentMembership) {

    public static MemberSummaryResponse of(Member m, MembershipResponse current) {
        return new MemberSummaryResponse(m.id(), m.fullName(), m.dni(), m.phone(), m.active(), current);
    }
}
