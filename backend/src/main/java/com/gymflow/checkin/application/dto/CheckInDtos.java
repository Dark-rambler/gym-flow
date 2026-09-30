package com.gymflow.checkin.application.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.gymflow.checkin.domain.model.CheckIn;
import com.gymflow.checkin.domain.model.DenyReason;
import com.gymflow.membership.domain.model.MembershipStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CheckInDtos {

    private CheckInDtos() {
    }

    /** code: contenido del QR ("GF1:&lt;uuid&gt;") o DNI tecleado. */
    public record CheckInRequest(@NotBlank(message = "Escanea el QR o escribe el DNI") @Size(max = 100) String code) {
    }

    public record CheckInMemberResponse(Long id, String fullName) {
    }

    public record CheckInMembershipResponse(String planName, LocalDate endDate, long daysLeft, MembershipStatus status) {
    }

    /**
     * duplicate = ya había entrado hace menos de 2 h: se muestra como permitido pero no cuenta otra asistencia;
     * checkedAt es entonces la hora de esa primera entrada.
     */
    public record CheckInResponse(
            CheckIn.Result result,
            boolean duplicate,
            @Schema(nullable = true) DenyReason reason,
            String message,
            @Schema(nullable = true) CheckInMemberResponse member,
            @Schema(nullable = true) CheckInMembershipResponse membership,
            Instant checkedAt) {
    }

    public record CheckInEntryResponse(
            Long id,
            Instant checkedAt,
            @Schema(nullable = true) Long memberId,
            @Schema(nullable = true) String memberName,
            CheckIn.Method method,
            CheckIn.Result result,
            @Schema(nullable = true) DenyReason reason) {
    }
}
