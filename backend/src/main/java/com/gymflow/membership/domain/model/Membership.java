package com.gymflow.membership.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.gymflow.shared.domain.exception.ConflictException;

/**
 * Membresía vendida a un socio. Fechas inclusivas: vale desde startDate hasta endDate (ambos incluidos).
 * storedStatus es lo que se persiste (ACTIVE | FROZEN | CANCELLED); el estado visible lo da statusOn(hoy).
 */
public record Membership(
        Long id,
        Long gymId,
        Long memberId,
        Long planId,
        String planName,
        BigDecimal price,
        LocalDate startDate,
        LocalDate endDate,
        MembershipStatus storedStatus,
        LocalDate frozenSince,
        int frozenDays,
        Long createdBy,
        Instant createdAt) {

    public static Membership sell(Long gymId, Long memberId, Long planId, String planName, BigDecimal price,
                                  int durationDays, LocalDate start, Long createdBy) {
        return new Membership(null, gymId, memberId, planId, planName, price, start,
                start.plusDays(durationDays - 1L), MembershipStatus.ACTIVE, null, 0, createdBy, null);
    }

    public MembershipStatus statusOn(LocalDate today) {
        if (storedStatus == MembershipStatus.CANCELLED) return MembershipStatus.CANCELLED;
        if (storedStatus == MembershipStatus.FROZEN) return MembershipStatus.FROZEN;
        if (today.isBefore(startDate)) return MembershipStatus.SCHEDULED;
        if (today.isAfter(endDate)) return MembershipStatus.EXPIRED;
        return MembershipStatus.ACTIVE;
    }

    /** Días que le quedan contando hoy (0 si ya venció o está cancelada). */
    public long daysLeft(LocalDate today) {
        if (storedStatus == MembershipStatus.CANCELLED || today.isAfter(endDate)) return 0;
        LocalDate from = today.isBefore(startDate) ? startDate : today;
        return ChronoUnit.DAYS.between(from, endDate) + 1;
    }

    /** Cuenta para el encadenado de renovaciones: todo lo no cancelado. */
    public boolean countsForChain() {
        return storedStatus != MembershipStatus.CANCELLED;
    }

    public Membership freeze(LocalDate today) {
        if (statusOn(today) != MembershipStatus.ACTIVE) {
            throw new ConflictException("Solo se puede congelar una membresía vigente");
        }
        return new Membership(id, gymId, memberId, planId, planName, price, startDate, endDate,
                MembershipStatus.FROZEN, today, frozenDays, createdBy, createdAt);
    }

    /** Descongela sumando al vencimiento los días que estuvo congelada. */
    public Membership unfreeze(LocalDate today) {
        if (storedStatus != MembershipStatus.FROZEN) {
            throw new ConflictException("La membresía no está congelada");
        }
        int days = frozenDaysUntil(today);
        return new Membership(id, gymId, memberId, planId, planName, price, startDate, endDate.plusDays(days),
                MembershipStatus.ACTIVE, null, frozenDays + days, createdBy, createdAt);
    }

    public int frozenDaysUntil(LocalDate today) {
        return frozenSince == null ? 0 : (int) Math.max(0, ChronoUnit.DAYS.between(frozenSince, today));
    }

    public Membership cancel() {
        if (storedStatus == MembershipStatus.CANCELLED) {
            throw new ConflictException("La membresía ya está cancelada");
        }
        return new Membership(id, gymId, memberId, planId, planName, price, startDate, endDate,
                MembershipStatus.CANCELLED, null, frozenDays, createdBy, createdAt);
    }
}
