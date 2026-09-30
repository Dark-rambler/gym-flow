package com.gymflow.membership.domain.model;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import com.gymflow.shared.domain.exception.ConflictException;

/** Reglas sobre el conjunto de membresías de un socio. */
public final class Memberships {

    private Memberships() {
    }

    /** Renovación encadenada: empieza hoy, o el día siguiente al último vencimiento si aún no venció. */
    public static LocalDate nextStartDate(Collection<Membership> existing, LocalDate today) {
        return existing.stream()
                .filter(Membership::countsForChain)
                .map(Membership::endDate)
                .max(Comparator.naturalOrder())
                .map(lastEnd -> lastEnd.plusDays(1))
                .filter(next -> next.isAfter(today))
                .orElse(today);
    }

    /**
     * Congelar solo si no hay renovaciones programadas; renovar solo si nada está congelado.
     * Así una membresía futura nunca "arranca" mientras otra está pausada y no hay que desplazar fechas.
     */
    public static void assertCanFreeze(Collection<Membership> existing, LocalDate today) {
        if (existing.stream().anyMatch(m -> m.statusOn(today) == MembershipStatus.SCHEDULED)) {
            throw new ConflictException("No se puede congelar: el socio tiene una renovación programada");
        }
    }

    /** Además, como máximo una renovación por adelantado (evita encadenar años por error o doble clic). */
    public static void assertCanRenew(Collection<Membership> existing, LocalDate today) {
        if (existing.stream().anyMatch(m -> m.statusOn(today) == MembershipStatus.FROZEN)) {
            throw new ConflictException("Descongela la membresía actual antes de renovar");
        }
        if (existing.stream().anyMatch(m -> m.statusOn(today) == MembershipStatus.SCHEDULED)) {
            throw new ConflictException("El socio ya tiene una renovación programada");
        }
    }

    /**
     * La membresía "actual" que se muestra en listados: la vigente o congelada; si no hay, la próxima programada;
     * si tampoco, la última que venció.
     */
    public static Optional<Membership> current(Collection<Membership> existing, LocalDate today) {
        var candidates = existing.stream().filter(Membership::countsForChain).toList();
        return candidates.stream()
                .filter(m -> m.statusOn(today) == MembershipStatus.ACTIVE || m.statusOn(today) == MembershipStatus.FROZEN)
                .findFirst()
                .or(() -> candidates.stream()
                        .filter(m -> m.statusOn(today) == MembershipStatus.SCHEDULED)
                        .min(Comparator.comparing(Membership::startDate)))
                .or(() -> candidates.stream().max(Comparator.comparing(Membership::endDate)));
    }
}
