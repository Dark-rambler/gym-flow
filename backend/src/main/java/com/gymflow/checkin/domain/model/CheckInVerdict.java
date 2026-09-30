package com.gymflow.checkin.domain.model;

import java.time.LocalDate;
import java.util.Optional;

import com.gymflow.member.domain.model.Member;
import com.gymflow.membership.domain.model.Membership;

/** ¿Puede entrar? Reutiliza el estado efectivo de la membresía (semana 2): solo ACTIVE hoy deja pasar. */
public record CheckInVerdict(boolean allowed, DenyReason reason) {

    private static final CheckInVerdict OK = new CheckInVerdict(true, null);

    public static CheckInVerdict evaluate(Member member, Optional<Membership> current, LocalDate today) {
        if (member == null) return deny(DenyReason.UNKNOWN);
        if (!member.active()) return deny(DenyReason.MEMBER_INACTIVE);
        if (current.isEmpty()) return deny(DenyReason.NO_MEMBERSHIP);
        return switch (current.get().statusOn(today)) {
            case ACTIVE -> OK;
            case SCHEDULED -> deny(DenyReason.NOT_STARTED);
            case FROZEN -> deny(DenyReason.FROZEN);
            case EXPIRED -> deny(DenyReason.EXPIRED);
            case CANCELLED -> deny(DenyReason.NO_MEMBERSHIP);
        };
    }

    private static CheckInVerdict deny(DenyReason reason) {
        return new CheckInVerdict(false, reason);
    }
}
