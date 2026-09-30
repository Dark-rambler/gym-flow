package com.gymflow.cash.infrastructure.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.gymflow.cash.domain.model.Payment;
import com.gymflow.cash.domain.port.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpa;

    @Override
    public Payment save(Payment p) {
        var e = new PaymentJpaEntity();
        e.setId(p.id());
        e.setGymId(p.gymId());
        e.setCashSessionId(p.cashSessionId());
        e.setMembershipId(p.membershipId());
        e.setMemberId(p.memberId());
        e.setAmount(p.amount());
        e.setMethod(p.method());
        e.setReference(p.reference());
        e.setReceivedBy(p.receivedBy());
        e.setPaidAt(p.paidAt());
        e.setIdempotencyKey(p.idempotencyKey());
        e.setVoidedAt(p.voidedAt());
        e.setVoidedBy(p.voidedBy());
        e.setVoidReason(p.voidReason());
        // flush inmediato: uk_payment_idempotency sale aquí y no al commit
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return jpa.findScopedById(id).map(PaymentRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Long> findMemberIdOf(Long paymentId) {
        return jpa.findMemberIdOf(paymentId);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(UUID key) {
        return jpa.findByIdempotencyKey(key).map(PaymentRepositoryAdapter::toDomain);
    }

    @Override
    public List<Payment> findBySession(Long cashSessionId) {
        return jpa.findByCashSessionIdOrderByPaidAtDescIdDesc(cashSessionId).stream()
                .map(PaymentRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<Payment> findBySessions(Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : jpa.findByCashSessionIdIn(ids).stream().map(PaymentRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<Payment> findByMemberships(Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : jpa.findByMembershipIdIn(ids).stream().map(PaymentRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<Payment> findValidBetween(Instant from, Instant to) {
        return jpa.findValidBetween(from, to).stream().map(PaymentRepositoryAdapter::toDomain).toList();
    }

    private static Payment toDomain(PaymentJpaEntity e) {
        return new Payment(e.getId(), e.getGymId(), e.getCashSessionId(), e.getMembershipId(), e.getMemberId(),
                e.getAmount(), e.getMethod(), e.getReference(), e.getReceivedBy(), e.getPaidAt(),
                e.getIdempotencyKey(), e.getVoidedAt(), e.getVoidedBy(), e.getVoidReason());
    }
}
