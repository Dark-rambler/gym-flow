package com.gymflow.cash.domain.port;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.gymflow.cash.domain.model.Payment;

/** Consultas filtradas por el gym actual (@TenantId). */
public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(Long id);

    /** Solo el socio del pago, sin cargar la entidad en la sesión (para bloquear antes de leer). */
    Optional<Long> findMemberIdOf(Long paymentId);

    Optional<Payment> findByIdempotencyKey(UUID key);

    /** Pagos de una caja, del más reciente al más antiguo. */
    List<Payment> findBySession(Long cashSessionId);

    List<Payment> findBySessions(Collection<Long> cashSessionIds);

    List<Payment> findByMemberships(Collection<Long> membershipIds);

    /** Pagos no anulados con paidAt en [from, to). */
    List<Payment> findValidBetween(Instant from, Instant to);
}
