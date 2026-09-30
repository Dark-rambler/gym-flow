package com.gymflow.cash.infrastructure.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Todo JPQL/derivado: el filtro @TenantId aplica siempre.
interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, Long> {

    @Query("select p from PaymentJpaEntity p where p.id = :id")
    Optional<PaymentJpaEntity> findScopedById(@Param("id") Long id);

    @Query("select p.memberId from PaymentJpaEntity p where p.id = :id")
    Optional<Long> findMemberIdOf(@Param("id") Long id);

    Optional<PaymentJpaEntity> findByIdempotencyKey(UUID key);

    List<PaymentJpaEntity> findByCashSessionIdOrderByPaidAtDescIdDesc(Long cashSessionId);

    List<PaymentJpaEntity> findByCashSessionIdIn(Collection<Long> ids);

    List<PaymentJpaEntity> findByMembershipIdIn(Collection<Long> ids);

    @Query("""
            select p from PaymentJpaEntity p
            where p.paidAt >= :from and p.paidAt < :to and p.voidedAt is null
            order by p.paidAt""")
    List<PaymentJpaEntity> findValidBetween(@Param("from") Instant from, @Param("to") Instant to);
}
