package com.gymflow.cash.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.gymflow.cash.domain.model.CashSession;
import com.gymflow.cash.domain.model.CashTotals;
import com.gymflow.cash.domain.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** DTOs de caja, pagos y reportes (records pequeños agrupados para no dispersarlos en 10 archivos). */
public final class CashDtos {

    private CashDtos() {
    }

    // tope de S/ 99,999.99: evita desbordar NUMERIC(10,2) al sumar el arqueo (una caja imposible de cerrar)
    public record OpenCashRequest(
            @NotNull(message = "El monto inicial es obligatorio") @DecimalMin(value = "0.00", message = "No puede ser negativo")
            @DecimalMax(value = "99999.99", message = "Monto demasiado alto")
            @Digits(integer = 5, fraction = 2, message = "Monto inválido") BigDecimal openingAmount) {
    }

    public record CloseCashRequest(
            @NotNull(message = "El efectivo contado es obligatorio") @DecimalMin(value = "0.00", message = "No puede ser negativo")
            @DecimalMax(value = "9999999.99", message = "Monto demasiado alto")
            @Digits(integer = 7, fraction = 2, message = "Monto inválido") BigDecimal countedCash,
            @Size(max = 500) String notes) {
    }

    public record VoidPaymentRequest(
            @NotBlank(message = "Indica el motivo de la anulación") @Size(max = 200) String reason) {
    }

    public record CashTotalsResponse(BigDecimal cash, BigDecimal yape, BigDecimal plin, BigDecimal card,
                                     BigDecimal total, int count) {

        public static CashTotalsResponse of(CashTotals t) {
            return new CashTotalsResponse(t.of(PaymentMethod.CASH), t.of(PaymentMethod.YAPE), t.of(PaymentMethod.PLIN),
                    t.of(PaymentMethod.CARD), t.total(), t.count());
        }
    }

    /**
     * expectedCash: en caja abierta se calcula en vivo; en cerrada es el valor congelado al cierre.
     * difference = contado − esperado (negativo = falta dinero).
     * Arqueo a ciegas: para RECEPTIONIST, expectedCash, totals y difference vienen null (ver CashViews.blind).
     */
    public record CashSessionResponse(
            Long id,
            CashSession.Status status,
            Instant openedAt,
            String openedByName,
            BigDecimal openingAmount,
            @Schema(nullable = true) BigDecimal expectedCash,
            @Schema(nullable = true) CashTotalsResponse totals,
            @Schema(nullable = true) Instant closedAt,
            @Schema(nullable = true) String closedByName,
            @Schema(nullable = true) BigDecimal countedCash,
            @Schema(nullable = true) BigDecimal difference,
            @Schema(nullable = true) String notes) {
    }

    public record PaymentResponse(
            Long id,
            Long memberId,
            String memberName,
            String planName,
            BigDecimal amount,
            PaymentMethod method,
            @Schema(nullable = true) String reference,
            String receivedByName,
            Instant paidAt,
            boolean voided,
            @Schema(nullable = true) String voidReason) {
    }

    public record CashSessionDetailResponse(CashSessionResponse session, List<PaymentResponse> payments) {

        /** Arqueo a ciegas: sin esperado, totales ni diferencia (los pagos individuales siguen visibles). */
        public CashSessionDetailResponse blind() {
            var s = session;
            return new CashSessionDetailResponse(new CashSessionResponse(s.id(), s.status(), s.openedAt(),
                    s.openedByName(), s.openingAmount(), null, null, s.closedAt(), s.closedByName(), s.countedCash(),
                    null, s.notes()), payments);
        }
    }

    /** current es null si no hay caja abierta. */
    public record CashStatusResponse(@Schema(nullable = true) CashSessionDetailResponse current) {
    }

    public record DailyIncomeResponse(LocalDate date, BigDecimal total, int count) {
    }

    public record IncomeReportResponse(LocalDate from, LocalDate to, CashTotalsResponse totals,
                                       List<DailyIncomeResponse> days) {
    }
}
