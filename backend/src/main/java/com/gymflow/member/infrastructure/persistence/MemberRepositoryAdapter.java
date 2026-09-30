package com.gymflow.member.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.gymflow.member.domain.model.Member;
import com.gymflow.member.domain.port.MemberRepository;
import com.gymflow.shared.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class MemberRepositoryAdapter implements MemberRepository {

    private final MemberJpaRepository jpa;

    @Override
    public Member save(Member member) {
        var e = new MemberJpaEntity();
        e.setId(member.id());
        e.setGymId(member.gymId());
        e.setFullName(member.fullName());
        e.setDni(member.dni());
        e.setPhone(member.phone());
        e.setEmail(member.email());
        e.setBirthDate(member.birthDate());
        e.setNotes(member.notes());
        e.setQrToken(member.qrToken());
        e.setActive(member.active());
        e.setCreatedAt(member.createdAt());
        // flush inmediato: uk_member_gym_dni sale aquí y no al commit
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public Optional<Member> findById(Long id) {
        return jpa.findScopedById(id).map(MemberRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Member> lockById(Long id) {
        return jpa.lockScopedById(id).map(MemberRepositoryAdapter::toDomain);
    }

    @Override
    public List<Member> findByIds(Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : jpa.findByIdIn(ids).stream().map(MemberRepositoryAdapter::toDomain).toList();
    }

    @Override
    public PageResult<Member> search(String query, int page, int size) {
        var pageable = PageRequest.of(page, size);
        Page<MemberJpaEntity> result;
        if (query == null || query.isBlank()) {
            result = jpa.findAllSorted(pageable);
        } else {
            String escaped = query.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            result = jpa.search("%" + escaped.toLowerCase(Locale.ROOT) + "%",
                    "%" + Member.normalizeDni(escaped) + "%", pageable);
        }
        return new PageResult<>(result.getContent().stream().map(MemberRepositoryAdapter::toDomain).toList(),
                page, size, result.getTotalElements());
    }

    private static Member toDomain(MemberJpaEntity e) {
        return new Member(e.getId(), e.getGymId(), e.getFullName(), e.getDni(), e.getPhone(), e.getEmail(),
                e.getBirthDate(), e.getNotes(), e.getQrToken(), e.isActive(), e.getCreatedAt());
    }
}
