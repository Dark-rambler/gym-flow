package com.gymflow.checkin.infrastructure.persistence;

import java.time.Instant;
import java.util.List;

import com.gymflow.checkin.domain.model.CheckIn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Todo JPQL: el filtro @TenantId aplica siempre.
interface CheckInJpaRepository extends JpaRepository<CheckInJpaEntity, Long> {

    @Query("""
            select c from CheckInJpaEntity c
            where c.memberId = :memberId and c.result = :result and c.checkedAt >= :since
            order by c.checkedAt desc""")
    List<CheckInJpaEntity> findRecent(@Param("memberId") Long memberId, @Param("result") CheckIn.Result result,
                                      @Param("since") Instant since, Pageable pageable);

    @Query("""
            select c from CheckInJpaEntity c
            where c.checkedAt >= :from and c.checkedAt < :to
            order by c.checkedAt desc, c.id desc""")
    Page<CheckInJpaEntity> findBetween(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query("""
            select count(c) from CheckInJpaEntity c
            where c.result = :result and c.checkedAt >= :from and c.checkedAt < :to""")
    long countBetween(@Param("result") CheckIn.Result result, @Param("from") Instant from, @Param("to") Instant to);
}
