package com.gymflow.staff.application.usecase;

import com.gymflow.auth.domain.model.AppUser;
import com.gymflow.auth.domain.port.PasswordHasher;
import com.gymflow.auth.domain.port.UserRepository;
import com.gymflow.shared.domain.exception.ForbiddenException;
import com.gymflow.shared.domain.model.Actor;
import com.gymflow.shared.domain.model.Role;
import com.gymflow.staff.application.dto.CreateStaffRequest;
import com.gymflow.staff.application.dto.StaffResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateStaffUseCase {

    private final UserRepository users;
    private final PasswordHasher hasher;

    @Transactional
    public StaffResponse execute(Actor actor, CreateStaffRequest req) {
        if (actor.is(Role.ADMIN) && req.role() != Role.RECEPTIONIST) {
            throw new ForbiddenException("Un ADMIN solo puede crear recepcionistas");
        }
        // El email duplicado (único global) lo detecta uk_app_user_email → 409 en GlobalExceptionHandler.
        AppUser user = users.save(AppUser.create(actor.gymId(), req.email(), hasher.hash(req.password()),
                req.fullName(), req.role()));
        return StaffResponse.of(user);
    }
}
