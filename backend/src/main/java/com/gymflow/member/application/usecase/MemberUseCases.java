package com.gymflow.member.application.usecase;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.gymflow.gym.application.GymCalendar;
import com.gymflow.member.application.dto.MemberDetailResponse;
import com.gymflow.member.application.dto.MemberRequest;
import com.gymflow.member.application.dto.MemberSummaryResponse;
import com.gymflow.member.domain.model.Member;
import com.gymflow.member.domain.port.MemberRepository;
import com.gymflow.membership.application.MembershipViews;
import com.gymflow.membership.domain.model.Membership;
import com.gymflow.membership.domain.model.Memberships;
import com.gymflow.membership.domain.port.MembershipRepository;
import com.gymflow.shared.application.dto.PageResponse;
import com.gymflow.shared.domain.exception.NotFoundException;
import com.gymflow.shared.domain.model.Actor;
import com.gymflow.shared.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberUseCases {

    public static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_QUERY_LENGTH = 100;

    private final MemberRepository members;
    private final MembershipRepository memberships;
    private final MembershipViews views;
    private final GymCalendar calendar;

    /** Listado paginado con la membresía actual de cada socio (una sola consulta extra para toda la página). */
    @Transactional(readOnly = true)
    public PageResponse<MemberSummaryResponse> search(Actor actor, String query, int page, int size) {
        String q = query != null && query.length() > MAX_QUERY_LENGTH ? query.substring(0, MAX_QUERY_LENGTH) : query;
        PageResult<Member> result = members.search(q, Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        LocalDate today = calendar.today(actor.gymId());
        List<Membership> all = memberships.findByMembers(result.items().stream().map(Member::id).toList());
        Map<Long, List<Membership>> byMember = all.stream().collect(Collectors.groupingBy(Membership::memberId));
        var toResponse = views.forMemberships(all, today);
        List<MemberSummaryResponse> items = result.items().stream()
                .map(m -> MemberSummaryResponse.of(m, Memberships.current(byMember.getOrDefault(m.id(), List.of()), today)
                        .map(toResponse).orElse(null)))
                .toList();
        return PageResponse.of(result, items);
    }

    @Transactional(readOnly = true)
    public MemberDetailResponse get(Actor actor, Long id) {
        return detail(actor, findOrThrow(id));
    }

    /** DNI duplicado en el gym → uk_member_gym_dni → 409 en GlobalExceptionHandler. */
    @Transactional
    public MemberDetailResponse create(Actor actor, MemberRequest req) {
        return detail(actor, members.save(Member.create(actor.gymId(), req.toData())));
    }

    // update y setActive reescriben la fila completa: con bloqueo, para que una edición no deshaga
    // una desactivación hecha a la vez (o viceversa).
    @Transactional
    public MemberDetailResponse update(Actor actor, Long id, MemberRequest req) {
        return detail(actor, members.save(lockOrThrow(id).update(req.toData())));
    }

    @Transactional
    public MemberDetailResponse setActive(Actor actor, Long id, boolean active) {
        return detail(actor, members.save(lockOrThrow(id).withActive(active)));
    }

    private Member findOrThrow(Long id) {
        return members.findById(id).orElseThrow(() -> new NotFoundException("Socio no encontrado"));
    }

    private Member lockOrThrow(Long id) {
        return members.lockById(id).orElseThrow(() -> new NotFoundException("Socio no encontrado"));
    }

    private MemberDetailResponse detail(Actor actor, Member member) {
        LocalDate today = calendar.today(actor.gymId());
        List<Membership> history = member.id() == null ? List.of() : memberships.findByMember(member.id());
        var toResponse = views.forMemberships(history, today);
        return MemberDetailResponse.of(member, Memberships.current(history, today).map(toResponse).orElse(null),
                history.stream().map(toResponse).toList());
    }
}
