package com.gymflow.membership.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.gymflow.membership.domain.model.Membership;
import com.gymflow.membership.domain.port.MembershipRepository;
import com.gymflow.membership.domain.model.MembershipStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class MembershipRepositoryAdapter implements MembershipRepository {

    private final MembershipJpaRepository jpa;

    @Override
    public Membership save(Membership m) {
        var e = new MembershipJpaEntity();
        e.setId(m.id());
        e.setGymId(m.gymId());
        e.setMemberId(m.memberId());
        e.setPlanId(m.planId());
        e.setPlanName(m.planName());
        e.setPrice(m.price());
        e.setStartDate(m.startDate());
        e.setEndDate(m.endDate());
        e.setStatus(m.storedStatus());
        e.setFrozenSince(m.frozenSince());
        e.setFrozenDays(m.frozenDays());
        e.setCreatedBy(m.createdBy());
        e.setCreatedAt(m.createdAt());
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public Optional<Membership> findById(Long id) {
        return jpa.findScopedById(id).map(MembershipRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Long> findMemberIdOf(Long membershipId) {
        return jpa.findMemberIdOf(membershipId);
    }

    @Override
    public List<Membership> findByMember(Long memberId) {
        return jpa.findByMemberIdOrderByStartDateDescIdDesc(memberId).stream()
                .map(MembershipRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<Membership> findByMembers(Collection<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return List.of();
        }
        return jpa.findByMemberIdIn(memberIds).stream().map(MembershipRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<Membership> findByIds(Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : jpa.findByIdIn(ids).stream().map(MembershipRepositoryAdapter::toDomain).toList();
    }

    @Override
    public long countMembersActiveOn(LocalDate day) {
        return jpa.countMembersWithStatusOn(MembershipStatus.ACTIVE, day);
    }

    @Override
    public long countMembersFrozen() {
        return jpa.countMembersWithStatus(MembershipStatus.FROZEN);
    }

    @Override
    public List<Membership> findExpiringWithoutRenewal(LocalDate from, LocalDate to, int limit) {
        return jpa.findExpiringWithoutRenewal(MembershipStatus.ACTIVE, MembershipStatus.CANCELLED, from, to,
                PageRequest.of(0, limit)).stream().map(MembershipRepositoryAdapter::toDomain).toList();
    }

    private static Membership toDomain(MembershipJpaEntity e) {
        return new Membership(e.getId(), e.getGymId(), e.getMemberId(), e.getPlanId(), e.getPlanName(), e.getPrice(),
                e.getStartDate(), e.getEndDate(), e.getStatus(), e.getFrozenSince(), e.getFrozenDays(),
                e.getCreatedBy(), e.getCreatedAt());
    }
}
