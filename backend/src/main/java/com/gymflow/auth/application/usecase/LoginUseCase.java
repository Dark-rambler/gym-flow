package com.gymflow.auth.application.usecase;

import java.util.UUID;

import com.gymflow.auth.application.dto.AuthResponse;
import com.gymflow.auth.application.dto.LoginRequest;
import com.gymflow.auth.domain.model.AppUser;
import com.gymflow.auth.domain.port.PasswordHasher;
import com.gymflow.auth.domain.port.UserRepository;
import com.gymflow.gym.domain.port.GymRepository;
import com.gymflow.shared.domain.exception.UnauthorizedException;
import com.gymflow.shared.infrastructure.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

// Login por email (único global) → corre como sistema para buscar el usuario en cualquier gym.
@Service
public class LoginUseCase {

    private static final String INVALID = "Email o contraseña incorrectos";

    private final UserRepository users;
    private final GymRepository gyms;
    private final PasswordHasher hasher;
    private final SessionIssuer sessions;
    private final TransactionTemplate tx;
    // hash de relleno para que un email inexistente tarde lo mismo que una contraseña incorrecta
    private final String dummyHash;

    public LoginUseCase(UserRepository users, GymRepository gyms, PasswordHasher hasher, SessionIssuer sessions,
                        TransactionTemplate tx) {
        this.users = users;
        this.gyms = gyms;
        this.hasher = hasher;
        this.sessions = sessions;
        this.tx = tx;
        this.dummyHash = hasher.hash(UUID.randomUUID().toString());
    }

    public AuthResponse execute(LoginRequest req) {
        return TenantContext.callAsSystem(() -> tx.execute(status -> {
            AppUser user = users.findByEmail(req.email()).orElse(null);
            boolean passwordOk = hasher.matches(req.password(), user != null ? user.passwordHash() : dummyHash);
            if (user == null || !passwordOk || !user.active()) {
                throw new UnauthorizedException(INVALID);
            }
            var gym = gyms.findById(user.gymId()).orElseThrow(() -> new UnauthorizedException(INVALID));
            return sessions.start(user, gym);
        }));
    }
}
