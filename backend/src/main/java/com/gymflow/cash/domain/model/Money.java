package com.gymflow.cash.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Montos en soles con 2 decimales, igual que NUMERIC(10,2) en BD ("145" y "145.00" son el mismo valor). */
public final class Money {

    private Money() {
    }

    public static BigDecimal of(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
