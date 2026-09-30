package com.gymflow.staff.application.usecase;

import java.time.Instant;

import com.gymflow.auth.domain.model.AppUser;
import com.gymflow.auth.domain.port.RefreshTokenRepository;
import com.gymflow.auth.domain.port.UserRepository;
import com.gymflow.shared.domain.exception.ConflictException;
import com.gymflow.shared.domain.exception.ForbiddenException;
import com.gymflow.shared.domain.exception.NotFoundException;
import com.gymflow.shared.domain.model.Actor;
import com.gymflow.shared.domain.model.Role;
import com.gymflow.staff.application.dto.StaffResponse;
import com.gymflow.staff.application.dto.UpdateStaffRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateStaffUseCase {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;

    @Transactional
    public StaffResponse execute(Actor actor, Long id, UpdateStaffRequest req) {
        // findById está filtrado por @TenantId: un id de otro gym da 404, igual que uno inexistente.
        AppUser target = users.findById(id).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        // Un ADMIN solo gestiona recepcionistas: no puede tocar a otros ADMIN/OWNER ni ascender a nadie.
        boolean onlyReceptionists = target.role() == Role.RECEPTIONIST
                && (req.role() == null || req.role() == Role.RECEPTIONIST);
        if (actor.is(Role.ADMIN) && !onlyReceptionists) {
            throw new ForbiddenException("Un ADMIN solo puede gestionar recepcionistas");
        }
        if (target.id().equals(actor.userId()) && Boolean.FALSE.equals(req.active())) {
            throw new ConflictException("No puedes desactivar tu propia cuenta");
        }

        boolean losesOwner = target.role() == Role.OWNER && target.active()
                && ((req.role() != null && req.role() != Role.OWNER) || Boolean.FALSE.equals(req.active()));
        // con bloqueo de filas: dos OWNER degradándose a la vez no pueden dejar el gym sin OWNER
        if (losesOwner && users.lockAndCountActiveByRole(Role.OWNER) <= 1) {
            throw new ConflictException("El gimnasio debe tener al menos un OWNER activo");
        }

        AppUser updated = target;
        if (req.role() != null) {
            updated = updated.withRole(req.role());
        }
        if (req.active() != null) {
            updated = updated.withActive(req.active());
        }
        updated = users.save(updated);

        if (!updated.active()) {
            refreshTokens.revokeAllActiveForUser(updated.id(), Instant.now());
        }
        return StaffResponse.of(updated);
    }
}
