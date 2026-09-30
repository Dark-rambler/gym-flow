package com.gymflow.staff.application.dto;

import java.time.Instant;

import com.gymflow.auth.domain.model.AppUser;
import com.gymflow.shared.domain.model.Role;

public record StaffResponse(Long id, String fullName, String email, Role role, boolean active, Instant createdAt) {

    public static StaffResponse of(AppUser u) {
        return new StaffResponse(u.id(), u.fullName(), u.email(), u.role(), u.active(), u.createdAt());
    }
}
