package com.gymflow.checkin.presentation;

import java.time.LocalDate;

import com.gymflow.checkin.application.dto.CheckInDtos.CheckInEntryResponse;
import com.gymflow.checkin.application.dto.CheckInDtos.CheckInRequest;
import com.gymflow.checkin.application.dto.CheckInDtos.CheckInResponse;
import com.gymflow.checkin.application.usecase.CheckInUseCases;
import com.gymflow.shared.application.dto.PageResponse;
import com.gymflow.shared.infrastructure.security.CurrentActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CheckIn")
@RestController
@RequestMapping("/api/check-ins")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInUseCases checkIns;

    @Operation(operationId = "checkIn", summary = "POST /api/check-ins — registra una entrada por QR o DNI (200 aunque se deniegue)")
    @PostMapping
    public CheckInResponse checkIn(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CheckInRequest req) {
        return checkIns.checkIn(CurrentActor.of(jwt), req.code());
    }

    @Operation(operationId = "listCheckIns", summary = "GET /api/check-ins — intentos de entrada de un día (hoy por defecto)")
    @GetMapping
    public PageResponse<CheckInEntryResponse> list(@AuthenticationPrincipal Jwt jwt,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "50") int size) {
        return checkIns.day(CurrentActor.of(jwt), date, page, size);
    }
}
