package com.gymflow.cash.application.usecase;

import java.math.BigDecimal;
import java.util.List;

import com.gymflow.cash.application.CashViews;
import com.gymflow.cash.application.dto.CashDtos.CashSessionDetailResponse;
import com.gymflow.cash.application.dto.CashDtos.CashSessionResponse;
import com.gymflow.cash.application.dto.CashDtos.CashStatusResponse;
import com.gymflow.cash.application.dto.CashDtos.CloseCashRequest;
import com.gymflow.cash.application.dto.CashDtos.PaymentResponse;
import com.gymflow.cash.domain.model.CashSession;
import com.gymflow.cash.domain.model.CashTotals;
import com.gymflow.cash.domain.model.Payment;
import com.gymflow.cash.domain.port.CashSessionRepository;
import com.gymflow.cash.domain.port.PaymentRepository;
import com.gymflow.gym.application.GymCalendar;
import com.gymflow.member.domain.port.MemberRepository;
import com.gymflow.membership.domain.model.Membership;
import com.gymflow.membership.domain.model.MembershipStatus;
import com.gymflow.membership.domain.port.MembershipRepository;
import com.gymflow.shared.application.dto.PageResponse;
import com.gymflow.shared.domain.exception.ConflictException;
import com.gymflow.shared.domain.exception.NotFoundException;
import com.gymflow.shared.domain.model.Actor;
import com.gymflow.shared.domain.model.PageResult;
import com.gymflow.shared.domain.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Caja del gimnasio: abrir, cerrar con arqueo, historial y anulación de pagos. Orden de bloqueo: socio → caja. */
@Service
@RequiredArgsConstructor
public class CashUseCases {

    private static final int MAX_PAGE_SIZE = 50;

    private final CashSessionRepository sessions;
    private final PaymentRepository payments;
    private final MembershipRepository memberships;
    private final MemberRepository members;
    private final CashViews views;
    private final GymCalendar calendar;

    @Transactional(readOnly = true)
    public CashStatusResponse current(Actor actor) {
        return new CashStatusResponse(sessions.findOpen().map(s -> forRole(actor, detail(s))).orElse(null));
    }

    /** Si ya hay una abierta → 409 (también lo garantiza el índice uk_cash_session_open ante carreras). */
    @Transactional
    public CashSessionDetailResponse open(Actor actor, BigDecimal openingAmount) {
        if (sessions.findOpen().isPresent()) {
            throw new ConflictException("Ya hay una caja abierta");
        }
        CashSession opened = sessions.save(CashSession.open(actor.gymId(), actor.userId(), openingAmount, calendar.now()));
        return forRole(actor, detail(opened));
    }

    /** Bloquea la caja: ninguna venta puede registrarse en ella mientras se cierra. */
    @Transactional
    public CashSessionDetailResponse close(Actor actor, CloseCashRequest req) {
        CashSession open = sessions.lockOpen().orElseThrow(() -> new ConflictException("No hay una caja abierta"));
        CashTotals totals = CashTotals.of(payments.findBySession(open.id()));
        CashSession closed = sessions.save(open.close(totals, req.countedCash(), actor.userId(), calendar.now(), req.notes()));
        return forRole(actor, detail(closed));
    }

    /**
     * Arqueo a ciegas: recepción no ve cuánto "debería" haber, así no puede ajustar el conteo para esconder un
     * faltante. La diferencia la ven OWNER/ADMIN en el historial.
     */
    private static CashSessionDetailResponse forRole(Actor actor, CashSessionDetailResponse detail) {
        return actor.is(Role.OWNER) || actor.is(Role.ADMIN) ? detail : detail.blind();
    }

    @Transactional(readOnly = true)
    public PageResponse<CashSessionResponse> history(int page, int size) {
        PageResult<CashSession> result = sessions.page(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        List<Payment> all = payments.findBySessions(result.items().stream().map(CashSession::id).toList());
        return PageResponse.of(result, views.sessions(result.items(), all));
    }

    @Transactional(readOnly = true)
    public CashSessionDetailResponse get(Long id) {
        return detail(sessions.findById(id).orElseThrow(() -> new NotFoundException("Caja no encontrada")));
    }

    /**
     * Anula un cobro hecho por error y cancela su membresía. Solo pagos de la caja abierta: una caja cerrada ya se
     * cuadró y no se toca. Bloquea socio → caja, igual que la venta.
     */
    @Transactional
    public PaymentResponse voidPayment(Actor actor, Long paymentId, String reason) {
        // solo el memberId (escalar) antes de bloquear: cargar el pago ahora dejaría una versión vieja en la sesión
        Long memberId = payments.findMemberIdOf(paymentId).orElseThrow(() -> new NotFoundException("Pago no encontrado"));
        members.lockById(memberId).orElseThrow(() -> new NotFoundException("Socio no encontrado"));
        CashSession open = sessions.lockOpen().orElse(null);

        Payment payment = payments.findById(paymentId).orElseThrow(() -> new NotFoundException("Pago no encontrado"));
        if (open == null || !open.id().equals(payment.cashSessionId())) {
            throw new ConflictException("Solo se pueden anular pagos de la caja abierta");
        }
        Membership membership = memberships.findById(payment.membershipId())
                .orElseThrow(() -> new NotFoundException("Membresía no encontrada"));
        // cancelar esta dejaría un hueco antes de la renovación ya programada (si ya estaba cancelada, el hueco ya
        // se aceptó al cancelarla y anular su pago no cambia nada)
        boolean alreadyCancelled = membership.storedStatus() == MembershipStatus.CANCELLED;
        boolean hasLaterRenewal = !alreadyCancelled && memberships.findByMember(memberId).stream()
                .anyMatch(m -> m.countsForChain() && m.startDate().isAfter(membership.endDate()));
        if (hasLaterRenewal) {
            throw new ConflictException("El socio tiene una renovación posterior: anula primero esa venta");
        }
        Payment voided = payments.save(payment.voidBy(actor.userId(), reason, calendar.now()));
        if (membership.storedStatus() != MembershipStatus.CANCELLED) {
            memberships.save(membership.cancel());
        }
        return views.payments(List.of(voided)).getFirst();
    }

    private CashSessionDetailResponse detail(CashSession s) {
        List<Payment> list = payments.findBySession(s.id());
        return new CashSessionDetailResponse(views.session(s, list), views.payments(list));
    }
}
