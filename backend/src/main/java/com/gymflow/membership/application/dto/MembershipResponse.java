package com.gymflow.membership.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.gymflow.cash.domain.model.Payment;
import com.gymflow.membership.domain.model.Membership;
import com.gymflow.membership.domain.model.MembershipStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/** status es el estado efectivo a la fecha de hoy del gym (SCHEDULED/ACTIVE/FROZEN/EXPIRED/CANCELLED). */
public record MembershipResponse(
        Long id,
        String planName,
        BigDecimal price,
        LocalDate startDate,
        LocalDate endDate,
        MembershipStatus status,
        @Schema(nullable = true) LocalDate frozenSince,
        int frozenDays,
        long daysLeft,
        @Schema(nullable = true) MembershipPaymentResponse payment) {

    /** Construir con MembershipViews, que carga los pagos por lote. */
    public static MembershipResponse of(Membership m, LocalDate today, Payment payment) {
        return new MembershipResponse(m.id(), m.planName(), m.price(), m.startDate(), m.endDate(), m.statusOn(today),
                m.frozenSince(), m.frozenDays(), m.daysLeft(today),
                payment == null ? null : MembershipPaymentResponse.of(payment));
    }
}
