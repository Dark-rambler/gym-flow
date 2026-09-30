package com.gymflow.cash.presentation;

import java.time.LocalDate;

import com.gymflow.cash.application.dto.CashDtos.CashSessionDetailResponse;
import com.gymflow.cash.application.dto.CashDtos.CashSessionResponse;
import com.gymflow.cash.application.dto.CashDtos.CashStatusResponse;
import com.gymflow.cash.application.dto.CashDtos.CloseCashRequest;
import com.gymflow.cash.application.dto.CashDtos.IncomeReportResponse;
import com.gymflow.cash.application.dto.CashDtos.OpenCashRequest;
import com.gymflow.cash.application.dto.CashDtos.PaymentResponse;
import com.gymflow.cash.application.dto.CashDtos.VoidPaymentRequest;
import com.gymflow.cash.application.usecase.CashUseCases;
import com.gymflow.cash.application.usecase.IncomeReportUseCase;
import com.gymflow.shared.application.dto.PageResponse;
import com.gymflow.shared.infrastructure.security.CurrentActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cash")
@RestController
@RequiredArgsConstructor
public class CashController {

    private final CashUseCases cash;
    private final IncomeReportUseCase reports;

    @Operation(operationId = "getCurrentCash", summary = "GET /api/cash/current — caja abierta con sus pagos (current null si está cerrada)")
    @GetMapping("/api/cash/current")
    @PreAuthorize("isAuthenticated()")
    public CashStatusResponse current(@AuthenticationPrincipal Jwt jwt) {
        return cash.current(CurrentActor.of(jwt));
    }

    @Operation(operationId = "openCash", summary = "POST /api/cash/open — abre la caja con un monto inicial")
    @PostMapping("/api/cash/open")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public CashSessionDetailResponse open(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OpenCashRequest req) {
        return cash.open(CurrentActor.of(jwt), req.openingAmount());
    }

    @Operation(operationId = "closeCash", summary = "POST /api/cash/close — cierra la caja con el efectivo contado (arqueo)")
    @PostMapping("/api/cash/close")
    @PreAuthorize("isAuthenticated()")
    public CashSessionDetailResponse close(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CloseCashRequest req) {
        return cash.close(CurrentActor.of(jwt), req);
    }

    @Operation(operationId = "listCashSessions", summary = "GET /api/cash/sessions — historial de cajas (page desde 0)")
    @GetMapping("/api/cash/sessions")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public PageResponse<CashSessionResponse> history(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return cash.history(page, size);
    }

    @Operation(operationId = "getCashSession", summary = "GET /api/cash/sessions/{id} — detalle de una caja con sus pagos")
    @GetMapping("/api/cash/sessions/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public CashSessionDetailResponse get(@PathVariable Long id) {
        return cash.get(id);
    }

    @Operation(operationId = "voidPayment", summary = "POST /api/payments/{id}/void — anula un pago de la caja abierta y cancela su membresía")
    @PostMapping("/api/payments/{id}/void")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public PaymentResponse voidPayment(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                       @Valid @RequestBody VoidPaymentRequest req) {
        return cash.voidPayment(CurrentActor.of(jwt), id, req.reason());
    }

    @Operation(operationId = "getIncomeReport", summary = "GET /api/reports/income — ingresos por día y método entre dos fechas (inclusive)")
    @GetMapping("/api/reports/income")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public IncomeReportResponse income(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.income(CurrentActor.of(jwt), from, to);
    }
}
