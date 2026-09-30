package com.gymflow.cash.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.gymflow.shared.domain.exception.ConflictException;

/** Turno de caja del gimnasio. Solo puede haber una abierta por gym (índice único parcial en V5). */
public record CashSession(
        Long id,
        Long gymId,
        Status status,
        Long openedBy,
        Instant openedAt,
        BigDecimal openingAmount,
        Long closedBy,
        Instant closedAt,
        BigDecimal countedCash,
        BigDecimal expectedCash,
        BigDecimal difference,
        String notes) {

    public enum Status { OPEN, CLOSED }

    public static CashSession open(Long gymId, Long userId, BigDecimal openingAmount, Instant now) {
        return new CashSession(null, gymId, Status.OPEN, userId, now, Money.of(openingAmount), null, null, null, null,
                null, null);
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    /** Arqueo: diferencia = contado − esperado (negativo = falta dinero). */
    public CashSession close(CashTotals totals, BigDecimal counted, Long userId, Instant now, String closingNotes) {
        if (!isOpen()) {
            throw new ConflictException("La caja ya está cerrada");
        }
        BigDecimal expected = Money.of(totals.expectedCash(openingAmount));
        BigDecimal countedCash = Money.of(counted);
        String n = closingNotes == null || closingNotes.isBlank() ? null : closingNotes.trim();
        return new CashSession(id, gymId, Status.CLOSED, openedBy, openedAt, openingAmount, userId, now, countedCash,
                expected, countedCash.subtract(expected), n);
    }
}
