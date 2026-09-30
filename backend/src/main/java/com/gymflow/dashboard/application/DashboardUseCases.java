package com.gymflow.dashboard.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.gymflow.checkin.application.usecase.CheckInUseCases;
import com.gymflow.gym.application.GymCalendar;
import com.gymflow.member.domain.model.Member;
import com.gymflow.member.domain.port.MemberRepository;
import com.gymflow.membership.domain.model.Membership;
import com.gymflow.membership.domain.port.MembershipRepository;
import com.gymflow.shared.domain.model.Actor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resumen del día para el dashboard (todos los roles; los ingresos van aparte, solo OWNER/ADMIN). */
@Service
@RequiredArgsConstructor
public class DashboardUseCases {

    public static final int EXPIRING_WITHIN_DAYS = 7;
    private static final int EXPIRING_LIMIT = 20;

    private final MembershipRepository memberships;
    private final MemberRepository members;
    private final CheckInUseCases checkIns;
    private final GymCalendar calendar;

    public record ExpiringMembershipResponse(Long memberId, String memberName, String planName, LocalDate endDate,
                                             long daysLeft) {
    }

    public record DashboardSummaryResponse(long activeMembers, long frozenMembers, long checkInsToday,
                                           List<ExpiringMembershipResponse> expiringSoon) {
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(Actor actor) {
        LocalDate today = calendar.today(actor.gymId());
        List<Membership> expiring = memberships.findExpiringWithoutRenewal(today,
                today.plusDays(EXPIRING_WITHIN_DAYS), EXPIRING_LIMIT);
        Map<Long, String> names = members.findByIds(expiring.stream().map(Membership::memberId).distinct().toList())
                .stream().collect(Collectors.toMap(Member::id, Member::fullName));
        return new DashboardSummaryResponse(
                memberships.countMembersActiveOn(today),
                memberships.countMembersFrozen(),
                checkIns.countToday(actor),
                expiring.stream().map(m -> new ExpiringMembershipResponse(m.memberId(),
                        names.getOrDefault(m.memberId(), "—"), m.planName(), m.endDate(), m.daysLeft(today))).toList());
    }
}
