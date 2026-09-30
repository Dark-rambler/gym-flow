package com.gymflow.cash.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.gymflow.cash.application.dto.CashDtos.CashTotalsResponse;
import com.gymflow.cash.application.dto.CashDtos.DailyIncomeResponse;
import com.gymflow.cash.application.dto.CashDtos.IncomeReportResponse;
import com.gymflow.cash.domain.model.CashTotals;
import com.gymflow.cash.domain.model.Payment;
import com.gymflow.cash.domain.port.PaymentRepository;
import com.gymflow.gym.application.GymCalendar;
import com.gymflow.shared.domain.exception.BadRequestException;
import com.gymflow.shared.domain.model.Actor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ingresos (pagos no anulados) entre dos fechas inclusivas, agrupados por día en la zona horaria del gym. */
@Service
@RequiredArgsConstructor
public class IncomeReportUseCase {

    private static final int MAX_DAYS = 366;
    // acota fechas absurdas (p. ej. +999999999-12-31) que harían fallar la aritmética de fechas con un 500
    private static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(2100, 12, 31);

    private final PaymentRepository payments;
    private final GymCalendar calendar;

    @Transactional(readOnly = true)
    public IncomeReportResponse income(Actor actor, LocalDate from, LocalDate to) {
        if (from.isBefore(MIN_DATE) || to.isAfter(MAX_DATE)) {
            throw new BadRequestException("Fechas fuera de rango");
        }
        if (to.isBefore(from)) {
            throw new BadRequestException("La fecha final no puede ser anterior a la inicial");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) {
            throw new BadRequestException("El rango máximo es de un año");
        }
        ZoneId zone = calendar.zone(actor.gymId());
        List<Payment> list = payments.findValidBetween(from.atStartOfDay(zone).toInstant(),
                to.plusDays(1).atStartOfDay(zone).toInstant());

        Map<LocalDate, List<Payment>> byDay = list.stream()
                .collect(Collectors.groupingBy(p -> LocalDate.ofInstant(p.paidAt(), zone), TreeMap::new, Collectors.toList()));
        List<DailyIncomeResponse> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            List<Payment> dayPayments = byDay.getOrDefault(d, List.of());
            BigDecimal total = dayPayments.stream().map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            days.add(new DailyIncomeResponse(d, total.setScale(2), dayPayments.size()));
        }
        return new IncomeReportResponse(from, to, CashTotalsResponse.of(CashTotals.of(list)), days);
    }
}
