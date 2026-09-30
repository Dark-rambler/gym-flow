package com.gymflow.cash.infrastructure.persistence;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Todo JPQL: el filtro @TenantId aplica siempre.
interface CashSessionJpaRepository extends JpaRepository<CashSessionJpaEntity, Long> {

    @Query("select c from CashSessionJpaEntity c where c.id = :id")
    Optional<CashSessionJpaEntity> findScopedById(@Param("id") Long id);

    @Query("select c from CashSessionJpaEntity c where c.status = com.gymflow.cash.domain.model.CashSession.Status.OPEN")
    Optional<CashSessionJpaEntity> findOpen();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CashSessionJpaEntity c where c.status = com.gymflow.cash.domain.model.CashSession.Status.OPEN")
    Optional<CashSessionJpaEntity> lockOpen();

    @Query("select c from CashSessionJpaEntity c order by c.openedAt desc, c.id desc")
    Page<CashSessionJpaEntity> findAllSorted(Pageable pageable);
}
