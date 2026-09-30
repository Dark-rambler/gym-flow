package com.gymflow.auth.application.dto;

import com.gymflow.auth.domain.model.AppUser;
import com.gymflow.gym.domain.model.Gym;
import com.gymflow.shared.domain.model.Role;

public record MeResponse(Long id, String fullName, String email, Role role, Long gymId, String gymName) {

    public static MeResponse of(AppUser user, Gym gym) {
        return new MeResponse(user.id(), user.fullName(), user.email(), user.role(), gym.id(), gym.name());
    }
}
