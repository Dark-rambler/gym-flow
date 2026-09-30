package com.gymflow.membership.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MembershipJpaRepository extends JpaRepository<MembershipJpaEntity, Long> {

    // JPQL para que el filtro @TenantId aplique a la búsqueda por id
    @Query("select m from MembershipJpaEntity m where m.id = :id")
    Optional<MembershipJpaEntity> findScopedById(@Param("id") Long id);

    @Query("select m.memberId from MembershipJpaEntity m where m.id = :id")
    Optional<Long> findMemberIdOf(@Param("id") Long id);

    List<MembershipJpaEntity> findByMemberIdOrderByStartDateDescIdDesc(Long memberId);

    List<MembershipJpaEntity> findByMemberIdIn(Collection<Long> memberIds);
}
