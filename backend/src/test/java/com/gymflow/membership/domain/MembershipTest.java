package com.gymflow.membership.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.gymflow.membership.domain.model.Membership;
import com.gymflow.membership.domain.model.MembershipStatus;
import com.gymflow.membership.domain.model.Memberships;
import com.gymflow.shared.domain.exception.ConflictException;
import org.junit.jupiter.api.Test;

class MembershipTest {

    private static final LocalDate JAN_1 = LocalDate.of(2026, 1, 1);

    private static Membership monthly(LocalDate start) {
        return Membership.sell(1L, 10L, 100L, "Mensual", new BigDecimal("100.00"), 30, start, 5L);
    }

    @Test
    void endDateIsInclusive() {
        assertThat(monthly(JAN_1).endDate()).isEqualTo(LocalDate.of(2026, 1, 30));
    }

    @Test
    void statusDependsOnDates() {
        Membership m = monthly(JAN_1);
        assertThat(m.statusOn(JAN_1.minusDays(1))).isEqualTo(MembershipStatus.SCHEDULED);
        assertThat(m.statusOn(JAN_1)).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(m.statusOn(LocalDate.of(2026, 1, 30))).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(m.statusOn(LocalDate.of(2026, 1, 31))).isEqualTo(MembershipStatus.EXPIRED);
        assertThat(m.cancel().statusOn(JAN_1)).isEqualTo(MembershipStatus.CANCELLED);
    }

    @Test
    void daysLeftCountsToday() {
        Membership m = monthly(JAN_1);
        assertThat(m.daysLeft(JAN_1)).isEqualTo(30);
        assertThat(m.daysLeft(LocalDate.of(2026, 1, 30))).isEqualTo(1);
        assertThat(m.daysLeft(LocalDate.of(2026, 1, 31))).isZero();
        assertThat(m.daysLeft(JAN_1.minusDays(5))).isEqualTo(30); // programada: días completos
    }

    @Test
    void renewalChainsAfterLastEndDateIgnoringCancelled() {
        Membership current = monthly(JAN_1);
        Membership cancelled = monthly(LocalDate.of(2026, 1, 31)).cancel();

        assertThat(Memberships.nextStartDate(List.of(current, cancelled), LocalDate.of(2026, 1, 10)))
                .isEqualTo(LocalDate.of(2026, 1, 31));
        // ya vencida: empieza hoy
        assertThat(Memberships.nextStartDate(List.of(current), LocalDate.of(2026, 3, 1)))
                .isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(Memberships.nextStartDate(List.of(), JAN_1)).isEqualTo(JAN_1);
    }

    @Test
    void freezeAndUnfreezeExtendsEndDate() {
        Membership frozen = monthly(JAN_1).freeze(LocalDate.of(2026, 1, 10));
        assertThat(frozen.statusOn(LocalDate.of(2026, 1, 15))).isEqualTo(MembershipStatus.FROZEN);

        Membership back = frozen.unfreeze(LocalDate.of(2026, 1, 20));

        assertThat(back.endDate()).isEqualTo(LocalDate.of(2026, 2, 9)); // +10 días
        assertThat(back.frozenDays()).isEqualTo(10);
        assertThat(back.statusOn(LocalDate.of(2026, 1, 20))).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    void cannotFreezeScheduledOrExpired() {
        Membership m = monthly(JAN_1);
        assertThatThrownBy(() -> m.freeze(JAN_1.minusDays(1))).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> m.freeze(LocalDate.of(2026, 2, 1))).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> m.unfreeze(JAN_1)).isInstanceOf(ConflictException.class);
    }

    @Test
    void cannotFreezeWithScheduledRenewalNorRenewWhileFrozen() {
        Membership jan = monthly(JAN_1);
        Membership feb = monthly(LocalDate.of(2026, 1, 31));
        LocalDate today = LocalDate.of(2026, 1, 10);

        assertThatThrownBy(() -> Memberships.assertCanFreeze(List.of(jan, feb), today))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> Memberships.assertCanRenew(List.of(jan.freeze(today)), today))
                .isInstanceOf(ConflictException.class);
        Memberships.assertCanFreeze(List.of(jan), today);
        Memberships.assertCanRenew(List.of(jan), today);
    }

    @Test
    void currentPrefersActiveThenScheduledThenLastExpired() {
        Membership jan = monthly(JAN_1);
        Membership feb = monthly(LocalDate.of(2026, 1, 31));

        assertThat(Memberships.current(List.of(feb, jan), LocalDate.of(2026, 1, 15))).contains(jan);
        assertThat(Memberships.current(List.of(feb), LocalDate.of(2026, 1, 15))).contains(feb);
        assertThat(Memberships.current(List.of(jan, feb), LocalDate.of(2026, 6, 1))).contains(feb);
        assertThat(Memberships.current(List.of(jan.cancel()), LocalDate.of(2026, 1, 15))).isEmpty();
    }
}
