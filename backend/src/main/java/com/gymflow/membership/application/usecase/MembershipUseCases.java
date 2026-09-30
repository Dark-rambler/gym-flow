package com.gymflow.membership.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.gymflow.gym.application.GymCalendar;
import com.gymflow.member.domain.model.Member;
import com.gymflow.member.domain.port.MemberRepository;
import com.gymflow.membership.application.dto.AssignMembershipRequest;
import com.gymflow.membership.application.dto.MembershipResponse;
import com.gymflow.membership.domain.model.Membership;
import com.gymflow.membership.domain.model.Memberships;
import com.gymflow.membership.domain.port.MembershipRepository;
import com.gymflow.plan.domain.model.MembershipPlan;
import com.gymflow.plan.domain.port.PlanRepository;
import com.gymflow.shared.domain.exception.ConflictException;
import com.gymflow.shared.domain.exception.ForbiddenException;
import com.gymflow.shared.domain.exception.NotFoundException;
import com.gymflow.shared.domain.model.Actor;
import com.gymflow.shared.domain.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Venta y ciclo de vida de membresías. Toda operación bloquea primero la fila del socio para que dos
 * ventas o congelados simultáneos no calculen fechas sobre el mismo estado.
 */
@Service
@RequiredArgsConstructor
public class MembershipUseCases {

    private final MembershipRepository memberships;
    private final MemberRepository members;
    private final PlanRepository plans;
    private final GymCalendar calendar;

    /** Asigna o renueva: empieza hoy o el día siguiente al último vencimiento (encadenado). */
    @Transactional
    public MembershipResponse assign(Actor actor, Long memberId, AssignMembershipRequest req) {
        if (req.price() != null && !(actor.is(Role.OWNER) || actor.is(Role.ADMIN))) {
            throw new ForbiddenException("Solo un dueño o administrador puede cambiar el precio");
        }
        Member member = lockMember(memberId);
        if (!member.active()) {
            throw new ConflictException("El socio está desactivado");
        }
        MembershipPlan plan = plans.findById(req.planId())
                .orElseThrow(() -> new NotFoundException("Plan no encontrado"));
        if (!plan.active()) {
            throw new ConflictException("El plan está desactivado");
        }

        LocalDate today = calendar.today(actor.gymId());
        List<Membership> existing = memberships.findByMember(memberId);
        Memberships.assertCanRenew(existing, today);
        BigDecimal price = req.price() != null ? req.price() : plan.price();
        Membership sold = Membership.sell(actor.gymId(), memberId, plan.id(), plan.name(), price, plan.durationDays(),
                Memberships.nextStartDate(existing, today), actor.userId());
        return MembershipResponse.of(memberships.save(sold), today);
    }

    @Transactional
    public MembershipResponse freeze(Actor actor, Long membershipId) {
        Membership m = loadAndLockMember(membershipId);
        LocalDate today = calendar.today(actor.gymId());
        Memberships.assertCanFreeze(memberships.findByMember(m.memberId()), today);
        return MembershipResponse.of(memberships.save(m.freeze(today)), today);
    }

    @Transactional
    public MembershipResponse unfreeze(Actor actor, Long membershipId) {
        Membership m = loadAndLockMember(membershipId);
        LocalDate today = calendar.today(actor.gymId());
        return MembershipResponse.of(memberships.save(m.unfreeze(today)), today);
    }

    @Transactional
    public MembershipResponse cancel(Actor actor, Long membershipId) {
        Membership m = loadAndLockMember(membershipId);
        LocalDate today = calendar.today(actor.gymId());
        return MembershipResponse.of(memberships.save(m.cancel()), today);
    }

    private Member lockMember(Long memberId) {
        return members.lockById(memberId).orElseThrow(() -> new NotFoundException("Socio no encontrado"));
    }

    /**
     * Bloquea al socio ANTES de cargar la membresía: si se cargara primero, la sesión de Hibernate se quedaría
     * con la versión previa al bloqueo y no veríamos cambios de otra transacción que terminó mientras esperábamos.
     */
    private Membership loadAndLockMember(Long membershipId) {
        Long memberId = memberships.findMemberIdOf(membershipId)
                .orElseThrow(() -> new NotFoundException("Membresía no encontrada"));
        lockMember(memberId);
        return memberships.findById(membershipId).orElseThrow(() -> new NotFoundException("Membresía no encontrada"));
    }
}
