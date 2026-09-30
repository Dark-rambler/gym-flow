package com.gymflow.cash.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.gymflow.shared.domain.exception.ConflictException;

/** Cobro de una membresía, registrado en la caja abierta. Un pago anulado no cuenta en ningún total. */
public record Payment(
        Long id,
        Long gymId,
        Long cashSessionId,
        Long membershipId,
        Long memberId,
        BigDecimal amount,
        PaymentMethod method,
        String reference,
        Long receivedBy,
        Instant paidAt,
        UUID idempotencyKey,
        Instant voidedAt,
        Long voidedBy,
        String voidReason) {

    public static Payment record(Long gymId, Long cashSessionId, Long membershipId, Long memberId, BigDecimal amount,
                                 PaymentMethod method, String reference, Long receivedBy, Instant now,
                                 UUID idempotencyKey) {
        String ref = reference == null || reference.isBlank() ? null : reference.trim();
        return new Payment(null, gymId, cashSessionId, membershipId, memberId, Money.of(amount), method, ref, receivedBy,
                now, idempotencyKey, null, null, null);
    }

    public boolean voided() {
        return voidedAt != null;
    }

    public Payment voidBy(Long userId, String reason, Instant now) {
        if (voided()) {
            throw new ConflictException("El pago ya está anulado");
        }
        return new Payment(id, gymId, cashSessionId, membershipId, memberId, amount, method, reference, receivedBy,
                paidAt, idempotencyKey, now, userId, reason.trim());
    }
}
