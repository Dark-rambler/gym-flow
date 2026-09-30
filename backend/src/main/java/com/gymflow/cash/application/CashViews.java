package com.gymflow.cash.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gymflow.auth.domain.port.UserRepository;
import com.gymflow.cash.application.dto.CashDtos.CashSessionResponse;
import com.gymflow.cash.application.dto.CashDtos.CashTotalsResponse;
import com.gymflow.cash.application.dto.CashDtos.PaymentResponse;
import com.gymflow.cash.domain.model.CashSession;
import com.gymflow.cash.domain.model.CashTotals;
import com.gymflow.cash.domain.model.Payment;
import com.gymflow.member.domain.model.Member;
import com.gymflow.member.domain.port.MemberRepository;
import com.gymflow.membership.domain.model.Membership;
import com.gymflow.membership.domain.port.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Arma las respuestas de caja resolviendo nombres (socio, plan, staff) con una consulta por tipo, no por fila. */
@Component
@RequiredArgsConstructor
public class CashViews {

    private static final String UNKNOWN = "—";

    private final UserRepository users;
    private final MemberRepository members;
    private final MembershipRepository memberships;

    public CashSessionResponse session(CashSession s, List<Payment> sessionPayments) {
        return sessions(List.of(s), sessionPayments).getFirst();
    }

    /** Varias cajas con sus totales; payments puede contener pagos de todas ellas. */
    public List<CashSessionResponse> sessions(List<CashSession> list, Collection<Payment> payments) {
        Map<Long, List<Payment>> bySession = payments.stream().collect(Collectors.groupingBy(Payment::cashSessionId));
        Set<Long> userIds = new HashSet<>();
        list.forEach(s -> {
            userIds.add(s.openedBy());
            if (s.closedBy() != null) userIds.add(s.closedBy());
        });
        Map<Long, String> names = users.findNamesByIds(userIds);
        List<CashSessionResponse> result = new ArrayList<>();
        for (CashSession s : list) {
            CashTotals totals = CashTotals.of(bySession.getOrDefault(s.id(), List.of()));
            result.add(new CashSessionResponse(s.id(), s.status(), s.openedAt(),
                    names.getOrDefault(s.openedBy(), UNKNOWN), s.openingAmount(),
                    s.isOpen() ? totals.expectedCash(s.openingAmount()) : s.expectedCash(),
                    CashTotalsResponse.of(totals), s.closedAt(),
                    s.closedBy() == null ? null : names.getOrDefault(s.closedBy(), UNKNOWN),
                    s.countedCash(), s.difference(), s.notes()));
        }
        return result;
    }

    public List<PaymentResponse> payments(List<Payment> list) {
        Map<Long, String> memberNames = members.findByIds(list.stream().map(Payment::memberId).distinct().toList())
                .stream().collect(Collectors.toMap(Member::id, Member::fullName));
        Map<Long, String> planNames = memberships.findByIds(list.stream().map(Payment::membershipId).distinct().toList())
                .stream().collect(Collectors.toMap(Membership::id, Membership::planName));
        Map<Long, String> staffNames = users.findNamesByIds(list.stream().map(Payment::receivedBy).collect(Collectors.toSet()));
        Function<Payment, PaymentResponse> toResponse = p -> new PaymentResponse(p.id(), p.memberId(),
                memberNames.getOrDefault(p.memberId(), UNKNOWN), planNames.getOrDefault(p.membershipId(), UNKNOWN),
                p.amount(), p.method(), p.reference(), staffNames.getOrDefault(p.receivedBy(), UNKNOWN), p.paidAt(),
                p.voided(), p.voidReason());
        return list.stream().map(toResponse).toList();
    }
}
