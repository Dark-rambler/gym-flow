package com.gymflow.auth.domain.port;

import java.util.List;
import java.util.Optional;

import com.gymflow.auth.domain.model.AppUser;
import com.gymflow.shared.domain.model.Role;

/**
 * Acceso a usuarios. Todas las consultas quedan filtradas por el gym actual (Hibernate @TenantId),
 * salvo que se ejecuten dentro de TenantContext.callAsSystem.
 */
public interface UserRepository {

    AppUser save(AppUser user);

    Optional<AppUser> findById(Long id);

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    List<AppUser> findAll();

    /** Bloquea (SELECT ... FOR UPDATE) los usuarios activos con ese rol y los cuenta. Requiere transacción. */
    long lockAndCountActiveByRole(Role role);
}
