package com.gymflow.member.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, Long> {

    // JPQL para que el filtro @TenantId aplique a la búsqueda por id
    @Query("select m from MemberJpaEntity m where m.id = :id")
    Optional<MemberJpaEntity> findScopedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MemberJpaEntity m where m.id = :id")
    Optional<MemberJpaEntity> lockScopedById(@Param("id") Long id);

    List<MemberJpaEntity> findByIdIn(Collection<Long> ids);

    @Query("select m from MemberJpaEntity m order by lower(m.fullName), m.id")
    Page<MemberJpaEntity> findAllSorted(Pageable pageable);

    @Query("""
            select m from MemberJpaEntity m
            where lower(m.fullName) like :namePattern escape '\\' or m.dni like :dniPattern escape '\\'
            order by lower(m.fullName), m.id""")
    Page<MemberJpaEntity> search(@Param("namePattern") String namePattern, @Param("dniPattern") String dniPattern,
                                 Pageable pageable);
}
