package com.gymflow.membership.application;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gymflow.cash.domain.model.Payment;
import com.gymflow.cash.domain.port.PaymentRepository;
import com.gymflow.membership.application.dto.MembershipResponse;
import com.gymflow.membership.domain.model.Membership;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Arma MembershipResponse con su pago, cargando los pagos de todas las membresías en una sola consulta. */
@Component
@RequiredArgsConstructor
public class MembershipViews {

    private final PaymentRepository payments;

    public Function<Membership, MembershipResponse> forMemberships(Collection<Membership> memberships, LocalDate today) {
        Map<Long, Payment> byMembership = payments
                .findByMemberships(memberships.stream().map(Membership::id).toList()).stream()
                // si hubiera más de uno, el vigente (no anulado) y más reciente
                .sorted(Comparator.comparing(Payment::voided).reversed().thenComparing(Payment::paidAt))
                .collect(Collectors.toMap(Payment::membershipId, p -> p, (a, b) -> b));
        return m -> MembershipResponse.of(m, today, byMembership.get(m.id()));
    }

    public MembershipResponse one(Membership membership, LocalDate today) {
        return forMemberships(List.of(membership), today).apply(membership);
    }
}
