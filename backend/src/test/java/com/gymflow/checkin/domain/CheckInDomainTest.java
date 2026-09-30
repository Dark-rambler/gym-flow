package com.gymflow.checkin.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.gymflow.checkin.domain.model.CheckInCode;
import com.gymflow.checkin.domain.model.CheckInVerdict;
import com.gymflow.checkin.domain.model.DenyReason;
import com.gymflow.member.domain.model.Member;
import com.gymflow.membership.domain.model.Membership;
import org.junit.jupiter.api.Test;

class CheckInDomainTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    private static final UUID TOKEN = UUID.fromString("3f2b8c1e-5a6d-4e7f-9a0b-1c2d3e4f5a6b");

    @Test
    void parsesQrPayloadBareUuidAndDni() {
        assertThat(CheckInCode.parse("GF1:" + TOKEN)).isEqualTo(new CheckInCode.Qr(TOKEN));
        assertThat(CheckInCode.parse("  gf1:" + TOKEN.toString().toUpperCase() + " ")).isEqualTo(new CheckInCode.Qr(TOKEN));
        assertThat(CheckInCode.parse(TOKEN.toString())).isEqualTo(new CheckInCode.Qr(TOKEN));
        assertThat(CheckInCode.parse(" 4012 3456 ")).isEqualTo(new CheckInCode.Dni("40123456"));
        assertThat(CheckInCode.parse("ab123456")).isEqualTo(new CheckInCode.Dni("AB123456"));
    }

    @Test
    void rejectsGarbage() {
        assertThat(CheckInCode.parse("")).isInstanceOf(CheckInCode.Invalid.class);
        assertThat(CheckInCode.parse("12")).isInstanceOf(CheckInCode.Invalid.class);
        assertThat(CheckInCode.parse("GF1:no-es-uuid")).isInstanceOf(CheckInCode.Invalid.class);
        assertThat(CheckInCode.parse("DROP TABLE;")).isInstanceOf(CheckInCode.Invalid.class);
        assertThat(CheckInCode.parse("x".repeat(200))).isInstanceOf(CheckInCode.Invalid.class);
    }

    @Test
    void verdictReusesMembershipStatus() {
        Member member = Member.create(1L, new Member.MemberData("Ana", "40123456", null, null, null, null));
        Membership march = Membership.sell(1L, 1L, 1L, "Mensual", BigDecimal.TEN, 30, LocalDate.of(2026, 3, 1), 1L);

        assertThat(CheckInVerdict.evaluate(member, Optional.of(march), TODAY).allowed()).isTrue();
        assertThat(reason(member, Optional.of(march), LocalDate.of(2026, 2, 28))).isEqualTo(DenyReason.NOT_STARTED);
        assertThat(reason(member, Optional.of(march), LocalDate.of(2026, 4, 1))).isEqualTo(DenyReason.EXPIRED);
        assertThat(reason(member, Optional.of(march.freeze(TODAY)), TODAY)).isEqualTo(DenyReason.FROZEN);
        assertThat(reason(member, Optional.empty(), TODAY)).isEqualTo(DenyReason.NO_MEMBERSHIP);
        assertThat(reason(member.withActive(false), Optional.of(march), TODAY)).isEqualTo(DenyReason.MEMBER_INACTIVE);
        assertThat(reason(null, Optional.empty(), TODAY)).isEqualTo(DenyReason.UNKNOWN);
    }

    private static DenyReason reason(Member m, Optional<Membership> current, LocalDate today) {
        return CheckInVerdict.evaluate(m, current, today).reason();
    }
}
