package com.gymflow.membership.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.gymflow.membership.domain.model.MembershipStatus;
import org.springframework.data.domain.Pageable;
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

    List<MembershipJpaEntity> findByIdIn(Collection<Long> ids);

    @Query("""
            select count(distinct m.memberId) from MembershipJpaEntity m
            where m.status = :status and m.startDate <= :day and m.endDate >= :day""")
    long countMembersWithStatusOn(@Param("status") MembershipStatus status, @Param("day") LocalDate day);

    @Query("select count(distinct m.memberId) from MembershipJpaEntity m where m.status = :status")
    long countMembersWithStatus(@Param("status") MembershipStatus status);

    // la subconsulta también pasa por el filtro @TenantId
    @Query("""
            select m from MembershipJpaEntity m
            where m.status = :active and m.startDate <= :from and m.endDate between :from and :to
              and exists (select s.id from MemberJpaEntity s where s.id = m.memberId and s.active = true)
              and not exists (
                select n.id from MembershipJpaEntity n
                where n.memberId = m.memberId and n.status <> :cancelled and n.startDate > m.endDate)
            order by m.endDate, m.id""")
    List<MembershipJpaEntity> findExpiringWithoutRenewal(@Param("active") MembershipStatus active,
                                                         @Param("cancelled") MembershipStatus cancelled,
                                                         @Param("from") LocalDate from, @Param("to") LocalDate to,
                                                         Pageable pageable);
}
