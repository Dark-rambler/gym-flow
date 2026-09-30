package com.gymflow.membership.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.gymflow.cash.domain.model.Payment;
import com.gymflow.cash.domain.model.PaymentMethod;

/** Resumen del cobro de una membresía (las vendidas antes de la semana 3 no tienen). */
public record MembershipPaymentResponse(Long id, PaymentMethod method, BigDecimal amount, Instant paidAt, boolean voided) {

    public static MembershipPaymentResponse of(Payment p) {
        return new MembershipPaymentResponse(p.id(), p.method(), p.amount(), p.paidAt(), p.voided());
    }
}
