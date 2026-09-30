package com.gymflow.cash.domain.model;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/** Totales de un conjunto de pagos, ignorando los anulados. */
public record CashTotals(Map<PaymentMethod, BigDecimal> byMethod, BigDecimal total, int count) {

    public static CashTotals of(Collection<Payment> payments) {
        Map<PaymentMethod, BigDecimal> byMethod = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod m : PaymentMethod.values()) {
            byMethod.put(m, BigDecimal.ZERO.setScale(2));
        }
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        int count = 0;
        for (Payment p : payments) {
            if (p.voided()) continue;
            byMethod.merge(p.method(), p.amount(), BigDecimal::add);
            total = total.add(p.amount());
            count++;
        }
        return new CashTotals(Map.copyOf(byMethod), total, count);
    }

    public BigDecimal of(PaymentMethod method) {
        return byMethod.get(method);
    }

    /** Efectivo que debería haber en el cajón: monto inicial + cobros en efectivo. Yape/Plin/tarjeta no entran. */
    public BigDecimal expectedCash(BigDecimal openingAmount) {
        return openingAmount.add(of(PaymentMethod.CASH));
    }
}
