package com.gymflow.staff.application.dto;

import com.gymflow.shared.domain.model.Role;

// Parcial: los campos null no se modifican.
public record UpdateStaffRequest(Role role, Boolean active) {
}
