package com.gymflow.auth.application.usecase;

import com.gymflow.auth.application.dto.AuthResponse;
import com.gymflow.auth.application.dto.RegisterGymRequest;
import com.gymflow.auth.domain.model.AppUser;
import com.gymflow.auth.domain.port.PasswordHasher;
import com.gymflow.auth.domain.port.UserRepository;
import com.gymflow.gym.domain.model.Gym;
import com.gymflow.gym.domain.model.Slugs;
import com.gymflow.gym.domain.port.GymRepository;
import com.gymflow.shared.domain.exception.ConflictException;
import com.gymflow.shared.domain.model.Role;
import com.gymflow.shared.infrastructure.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

// Alta pública de un gimnasio + su OWNER. Corre como sistema: aún no hay tenant y el email se valida globalmente.
@Service
@RequiredArgsConstructor
public class RegisterGymUseCase {

    private final GymRepository gyms;
    private final UserRepository users;
    private final PasswordHasher hasher;
    private final SessionIssuer sessions;
    private final TransactionTemplate tx;

    public AuthResponse execute(RegisterGymRequest req) {
        return TenantContext.callAsSystem(() -> tx.execute(status -> {
            if (users.existsByEmail(req.email())) {
                throw new ConflictException("El email ya está registrado");
            }
            Gym gym = gyms.save(Gym.create(req.gymName(), uniqueSlug(req.gymName())));
            AppUser owner = users.save(AppUser.create(gym.id(), req.email(), hasher.hash(req.password()),
                    req.ownerName(), Role.OWNER));
            return sessions.start(owner, gym);
        }));
    }

    private String uniqueSlug(String name) {
        String base = Slugs.of(name);
        String slug = base;
        for (int i = 2; gyms.existsBySlug(slug); i++) {
            slug = base + "-" + i;
        }
        return slug;
    }
}
