package com.gymflow.checkin.application.usecase;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gymflow.checkin.application.dto.CheckInDtos.CheckInEntryResponse;
import com.gymflow.checkin.application.dto.CheckInDtos.CheckInMemberResponse;
import com.gymflow.checkin.application.dto.CheckInDtos.CheckInMembershipResponse;
import com.gymflow.checkin.application.dto.CheckInDtos.CheckInResponse;
import com.gymflow.checkin.domain.model.CheckIn;
import com.gymflow.checkin.domain.model.CheckInCode;
import com.gymflow.checkin.domain.model.CheckInVerdict;
import com.gymflow.checkin.domain.model.DenyReason;
import com.gymflow.checkin.domain.port.CheckInRepository;
import com.gymflow.gym.application.GymCalendar;
import com.gymflow.member.domain.model.Member;
import com.gymflow.member.domain.port.MemberRepository;
import com.gymflow.membership.domain.model.Membership;
import com.gymflow.membership.domain.model.Memberships;
import com.gymflow.membership.domain.port.MembershipRepository;
import com.gymflow.shared.application.dto.PageResponse;
import com.gymflow.shared.domain.model.Actor;
import com.gymflow.shared.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Control de acceso en recepción por QR o DNI. */
@Service
@RequiredArgsConstructor
public class CheckInUseCases {

    /** Otra lectura del mismo socio dentro de esta ventana es la misma visita. */
    public static final Duration SAME_VISIT = Duration.ofHours(2);
    private static final int MAX_PAGE_SIZE = 100;

    private final CheckInRepository checkIns;
    private final MemberRepository members;
    private final MembershipRepository memberships;
    private final GymCalendar calendar;

    @Transactional
    public CheckInResponse checkIn(Actor actor, String raw) {
        Instant now = calendar.now();
        CheckInCode code = CheckInCode.parse(raw);
        if (code instanceof CheckInCode.Invalid) {
            // basura del lector o un error de tipeo: no se registra
            return denied(DenyReason.INVALID_CODE, null, null, now);
        }
        CheckIn.Method method = code instanceof CheckInCode.Qr ? CheckIn.Method.QR : CheckIn.Method.DNI;
        // búsqueda filtrada por tenant: un QR/DNI de otro gym es "desconocido". Solo el id: el socio se lee
        // DESPUÉS de bloquearlo (si se cargara antes, la sesión se quedaría con un "active" viejo).
        Optional<Long> memberId = switch (code) {
            case CheckInCode.Qr qr -> members.findIdByQrToken(qr.token());
            case CheckInCode.Dni dni -> members.findIdByDni(dni.value());
            case CheckInCode.Invalid invalid -> Optional.empty();
        };
        // bloqueo del socio: dos lecturas simultáneas no cuentan como dos visitas
        Optional<Member> locked = memberId.flatMap(members::lockById);
        if (locked.isEmpty()) {
            checkIns.save(CheckIn.denied(actor.gymId(), null, null, method, DenyReason.UNKNOWN, now, actor.userId()));
            return denied(DenyReason.UNKNOWN, null, null, now);
        }
        Member member = locked.get();
        LocalDate today = calendar.today(actor.gymId());
        Optional<Membership> current = Memberships.current(memberships.findByMember(member.id()), today);
        CheckInVerdict verdict = CheckInVerdict.evaluate(member, current, today);
        CheckInMemberResponse memberInfo = new CheckInMemberResponse(member.id(), member.fullName());
        CheckInMembershipResponse membershipInfo = current.map(m -> membershipInfo(m, today)).orElse(null);
        Long membershipId = current.map(Membership::id).orElse(null);

        if (!verdict.allowed()) {
            checkIns.save(CheckIn.denied(actor.gymId(), member.id(), membershipId, method, verdict.reason(), now,
                    actor.userId()));
            return denied(verdict.reason(), memberInfo, membershipInfo, now);
        }
        Optional<CheckIn> sameVisit = checkIns.findLastAllowedSince(member.id(), now.minus(SAME_VISIT));
        if (sameVisit.isPresent()) {
            return new CheckInResponse(CheckIn.Result.ALLOWED, true, null, "Ya registró su entrada", memberInfo,
                    membershipInfo, sameVisit.get().checkedAt());
        }
        checkIns.save(CheckIn.allowed(actor.gymId(), member.id(), membershipId, method, now, actor.userId()));
        return new CheckInResponse(CheckIn.Result.ALLOWED, false, null, "Bienvenido/a, " + firstName(member),
                memberInfo, membershipInfo, now);
    }

    /** Intentos de un día (en la zona del gym), el más reciente primero. */
    @Transactional(readOnly = true)
    public PageResponse<CheckInEntryResponse> day(Actor actor, LocalDate date, int page, int size) {
        ZoneId zone = calendar.zone(actor.gymId());
        LocalDate day = date != null ? date : calendar.today(actor.gymId());
        PageResult<CheckIn> result = checkIns.findBetween(day.atStartOfDay(zone).toInstant(),
                day.plusDays(1).atStartOfDay(zone).toInstant(), Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        Map<Long, String> names = members.findByIds(result.items().stream()
                        .map(CheckIn::memberId).filter(id -> id != null).distinct().toList())
                .stream().collect(Collectors.toMap(Member::id, Member::fullName));
        List<CheckInEntryResponse> items = result.items().stream()
                .map(c -> new CheckInEntryResponse(c.id(), c.checkedAt(), c.memberId(),
                        c.memberId() == null ? null : names.get(c.memberId()), c.method(), c.result(), c.reason()))
                .toList();
        return PageResponse.of(result, items);
    }

    /** Entradas permitidas hoy (para el dashboard). */
    public long countToday(Actor actor) {
        ZoneId zone = calendar.zone(actor.gymId());
        LocalDate today = calendar.today(actor.gymId());
        return checkIns.countAllowedBetween(today.atStartOfDay(zone).toInstant(), today.plusDays(1).atStartOfDay(zone).toInstant());
    }

    private static CheckInResponse denied(DenyReason reason, CheckInMemberResponse member,
                                          CheckInMembershipResponse membership, Instant now) {
        return new CheckInResponse(CheckIn.Result.DENIED, false, reason, reason.message(), member, membership, now);
    }

    private static CheckInMembershipResponse membershipInfo(Membership m, LocalDate today) {
        return new CheckInMembershipResponse(m.planName(), m.endDate(), m.daysLeft(today), m.statusOn(today));
    }

    private static String firstName(Member m) {
        return m.fullName().split("\\s+")[0];
    }
}
