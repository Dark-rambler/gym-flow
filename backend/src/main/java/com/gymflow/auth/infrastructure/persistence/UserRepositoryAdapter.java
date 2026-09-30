package com.gymflow.auth.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gymflow.auth.domain.model.AppUser;
import com.gymflow.auth.domain.port.UserRepository;
import com.gymflow.shared.domain.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UserRepositoryAdapter implements UserRepository {

    private final AppUserJpaRepository jpa;
    private final UserPersistenceMapper mapper;

    @Override
    public AppUser save(AppUser user) {
        // flush inmediato: la violación de uk_app_user_email sale aquí y no al commit
        return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(user)));
    }

    @Override
    public Optional<AppUser> findById(Long id) {
        return jpa.findScopedById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AppUser> findByEmail(String email) {
        return jpa.findByEmail(AppUser.normalizeEmail(email)).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(AppUser.normalizeEmail(email));
    }

    @Override
    public List<AppUser> findAll() {
        return jpa.findAllByOrderByFullNameAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Map<Long, String> findNamesByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return jpa.findByIdIn(ids).stream()
                .collect(Collectors.toMap(AppUserJpaEntity::getId, AppUserJpaEntity::getFullName));
    }

    @Override
    public long lockAndCountActiveByRole(Role role) {
        return jpa.findByRoleAndActiveTrue(role).size();
    }
}
