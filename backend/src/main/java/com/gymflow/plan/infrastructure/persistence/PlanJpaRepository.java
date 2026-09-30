package com.gymflow.plan.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PlanJpaRepository extends JpaRepository<PlanJpaEntity, Long> {

    // JPQL para que el filtro @TenantId aplique a la búsqueda por id
    @Query("select p from PlanJpaEntity p where p.id = :id")
    Optional<PlanJpaEntity> findScopedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PlanJpaEntity p where p.id = :id")
    Optional<PlanJpaEntity> lockScopedById(@Param("id") Long id);

    List<PlanJpaEntity> findAllByOrderByDurationDaysAscNameAsc();

    List<PlanJpaEntity> findByActiveTrueOrderByDurationDaysAscNameAsc();
}
