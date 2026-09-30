package com.gymflow.auth.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.gymflow.shared.domain.model.Role;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AppUserJpaRepository extends JpaRepository<AppUserJpaEntity, Long> {

    // JPQL (no em.find) para que el filtro @TenantId aplique también a la búsqueda por id.
    @Query("select u from AppUserJpaEntity u where u.id = :id")
    Optional<AppUserJpaEntity> findScopedById(@Param("id") Long id);

    Optional<AppUserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    List<AppUserJpaEntity> findAllByOrderByFullNameAsc();

    List<AppUserJpaEntity> findByIdIn(Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AppUserJpaEntity> findByRoleAndActiveTrue(Role role);
}
