package com.gymflow.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    // Consumo atómico: de N peticiones concurrentes con el mismo token solo una obtiene 1.
    @Modifying(clearAutomatically = true)
    @Query("update RefreshTokenJpaEntity t set t.revokedAt = :now where t.id = :id and t.revokedAt is null")
    int consume(@Param("id") Long id, @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("update RefreshTokenJpaEntity t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
    int revokeAllActiveForUser(@Param("userId") Long userId, @Param("now") Instant now);
}
