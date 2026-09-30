package com.gymflow.cash.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.gymflow.cash.domain.model.CashSession;
import com.gymflow.cash.domain.model.CashTotals;
import com.gymflow.cash.domain.model.Payment;
import com.gymflow.cash.domain.model.PaymentMethod;
import com.gymflow.shared.domain.exception.ConflictException;
import org.junit.jupiter.api.Test;

class CashDomainTest {

    private static final Instant NOW = Instant.parse("2026-03-10T15:00:00Z");

    private static Payment pay(String amount, PaymentMethod method) {
        return Payment.record(1L, 1L, 1L, 1L, new BigDecimal(amount), method, null, 1L, NOW, UUID.randomUUID());
    }

    @Test
    void totalsByMethodIgnoreVoidedPayments() {
        var voided = pay("999.00", PaymentMethod.CASH).voidBy(2L, "error", NOW);
        CashTotals totals = CashTotals.of(List.of(
                pay("100.00", PaymentMethod.CASH), pay("50.00", PaymentMethod.CASH),
                pay("270.00", PaymentMethod.YAPE), pay("80.00", PaymentMethod.CARD), voided));

        assertThat(totals.of(PaymentMethod.CASH)).isEqualByComparingTo("150");
        assertThat(totals.of(PaymentMethod.YAPE)).isEqualByComparingTo("270");
        assertThat(totals.of(PaymentMethod.PLIN)).isEqualByComparingTo("0");
        assertThat(totals.total()).isEqualByComparingTo("500");
        assertThat(totals.count()).isEqualTo(4);
    }

    @Test
    void closeComputesExpectedCashAndDifference() {
        CashSession open = CashSession.open(1L, 1L, new BigDecimal("50.00"), NOW);
        CashTotals totals = CashTotals.of(List.of(pay("100.00", PaymentMethod.CASH), pay("270.00", PaymentMethod.YAPE)));

        CashSession closed = open.close(totals, new BigDecimal("145.00"), 2L, NOW, "  faltan 5  ");

        assertThat(closed.isOpen()).isFalse();
        assertThat(closed.expectedCash()).isEqualByComparingTo("150"); // 50 inicial + 100 efectivo (Yape no cuenta)
        assertThat(closed.difference()).isEqualByComparingTo("-5");
        assertThat(closed.notes()).isEqualTo("faltan 5");
        assertThatThrownBy(() -> closed.close(totals, BigDecimal.ONE, 2L, NOW, null)).isInstanceOf(ConflictException.class);
    }

    @Test
    void paymentCanBeVoidedOnlyOnce() {
        Payment p = pay("100.00", PaymentMethod.CASH).voidBy(2L, " cobro duplicado ", NOW);
        assertThat(p.voided()).isTrue();
        assertThat(p.voidReason()).isEqualTo("cobro duplicado");
        assertThatThrownBy(() -> p.voidBy(2L, "otra vez", NOW)).isInstanceOf(ConflictException.class);
    }
}
