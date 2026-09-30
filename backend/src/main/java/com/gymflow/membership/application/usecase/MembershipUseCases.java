package com.gymflow.membership.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.gymflow.cash.domain.model.CashSession;
import com.gymflow.cash.domain.model.Payment;
import com.gymflow.cash.domain.port.CashSessionRepository;
import com.gymflow.cash.domain.port.PaymentRepository;
import com.gymflow.gym.application.GymCalendar;
import com.gymflow.member.domain.model.Member;
import com.gymflow.member.domain.port.MemberRepository;
import com.gymflow.membership.application.MembershipViews;
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
 * Venta y ciclo de vida de membresías. Orden de bloqueo en toda la app: socio → caja.
 * Bloquear al socio primero evita que dos ventas o congelados simultáneos calculen fechas sobre el mismo estado.
 */
@Service
@RequiredArgsConstructor
public class MembershipUseCases {

    private final MembershipRepository memberships;
    private final MemberRepository members;
    private final PlanRepository plans;
    private final CashSessionRepository cashSessions;
    private final PaymentRepository payments;
    private final MembershipViews views;
    private final GymCalendar calendar;

    /**
     * Vende o renueva: crea la membresía encadenada y su pago en la caja abierta, en una sola transacción.
     * Idempotente por idempotencyKey: un reintento devuelve la venta original sin volver a cobrar.
     */
    @Transactional
    public MembershipResponse assign(Actor actor, Long memberId, AssignMembershipRequest req) {
        if (req.price() != null && !(actor.is(Role.OWNER) || actor.is(Role.ADMIN))) {
            throw new ForbiddenException("Solo un dueño o administrador puede cambiar el precio");
        }
        Member member = lockMember(memberId);
        LocalDate today = calendar.today(actor.gymId());

        Optional<Payment> previous = payments.findByIdempotencyKey(req.idempotencyKey());
        if (previous.isPresent()) {
            return replay(previous.get(), memberId, req, today);
        }

        CashSession cash = cashSessions.lockOpen()
                .orElseThrow(() -> new ConflictException("La caja está cerrada. Ábrela para poder cobrar"));
        if (!member.active()) {
            throw new ConflictException("El socio está desactivado");
        }
        MembershipPlan plan = plans.findById(req.planId())
                .orElseThrow(() -> new NotFoundException("Plan no encontrado"));
        if (!plan.active()) {
            throw new ConflictException("El plan está desactivado");
        }

        List<Membership> existing = memberships.findByMember(memberId);
        Memberships.assertCanRenew(existing, today);
        BigDecimal price = req.price() != null ? req.price() : plan.price();
        Membership sold = memberships.save(Membership.sell(actor.gymId(), memberId, plan.id(), plan.name(), price,
                plan.durationDays(), Memberships.nextStartDate(existing, today), actor.userId()));
        Payment payment = payments.save(Payment.record(actor.gymId(), cash.id(), sold.id(), memberId, price,
                req.paymentMethod(), req.paymentReference(), actor.userId(), calendar.now(), req.idempotencyKey()));
        return MembershipResponse.of(sold, today, payment);
    }

    /** Un reintento solo es válido si es exactamente la misma venta; si cambió algo, no se oculta tras un 201. */
    private MembershipResponse replay(Payment payment, Long memberId, AssignMembershipRequest req, LocalDate today) {
        Membership original = memberships.findById(payment.membershipId())
                .orElseThrow(() -> new NotFoundException("Membresía no encontrada"));
        // sin price en el request, el cliente espera el precio del plan: si la venta original fue con descuento,
        // no es la misma venta
        BigDecimal expectedPrice = req.price() != null ? req.price()
                : plans.findById(req.planId()).map(MembershipPlan::price).orElse(null);
        boolean sameSale = payment.memberId().equals(memberId)
                && original.planId().equals(req.planId())
                && payment.method() == req.paymentMethod()
                && expectedPrice != null && payment.amount().compareTo(expectedPrice) == 0;
        if (!sameSale) {
            throw new ConflictException("La clave de idempotencia ya se usó en otra venta");
        }
        return MembershipResponse.of(original, today, payment);
    }

    @Transactional
    public MembershipResponse freeze(Actor actor, Long membershipId) {
        Membership m = loadAndLockMember(membershipId);
        LocalDate today = calendar.today(actor.gymId());
        Memberships.assertCanFreeze(memberships.findByMember(m.memberId()), today);
        return views.one(memberships.save(m.freeze(today)), today);
    }

    @Transactional
    public MembershipResponse unfreeze(Actor actor, Long membershipId) {
        Membership m = loadAndLockMember(membershipId);
        LocalDate today = calendar.today(actor.gymId());
        return views.one(memberships.save(m.unfreeze(today)), today);
    }

    /** Cancela sin devolver dinero. Para revertir un cobro por error: anular el pago desde la caja. */
    @Transactional
    public MembershipResponse cancel(Actor actor, Long membershipId) {
        Membership m = loadAndLockMember(membershipId);
        LocalDate today = calendar.today(actor.gymId());
        return views.one(memberships.save(m.cancel()), today);
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
