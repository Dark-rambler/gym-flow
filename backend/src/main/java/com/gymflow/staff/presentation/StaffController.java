package com.gymflow.staff.presentation;

import java.util.List;

import com.gymflow.shared.infrastructure.security.CurrentActor;
import com.gymflow.staff.application.dto.CreateStaffRequest;
import com.gymflow.staff.application.dto.StaffResponse;
import com.gymflow.staff.application.dto.UpdateStaffRequest;
import com.gymflow.staff.application.usecase.CreateStaffUseCase;
import com.gymflow.staff.application.usecase.ListStaffUseCase;
import com.gymflow.staff.application.usecase.UpdateStaffUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Staff")
@RestController
@RequestMapping("/api/staff")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
@RequiredArgsConstructor
public class StaffController {

    private final ListStaffUseCase listStaff;
    private final CreateStaffUseCase createStaff;
    private final UpdateStaffUseCase updateStaff;

    @Operation(operationId = "listStaff", summary = "GET /api/staff — staff del gimnasio")
    @GetMapping
    public List<StaffResponse> list() {
        return listStaff.execute();
    }

    @Operation(operationId = "createStaff", summary = "POST /api/staff — crea un usuario del staff con contraseña inicial")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateStaffRequest req) {
        return createStaff.execute(CurrentActor.of(jwt), req);
    }

    @Operation(operationId = "updateStaff", summary = "PATCH /api/staff/{id} — cambia rol y/o estado activo")
    @PatchMapping("/{id}")
    public StaffResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                @RequestBody UpdateStaffRequest req) {
        return updateStaff.execute(CurrentActor.of(jwt), id, req);
    }
}
