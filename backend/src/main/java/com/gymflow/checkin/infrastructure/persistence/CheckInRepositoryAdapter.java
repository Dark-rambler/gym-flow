package com.gymflow.checkin.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;

import com.gymflow.checkin.domain.model.CheckIn;
import com.gymflow.checkin.domain.port.CheckInRepository;
import com.gymflow.shared.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class CheckInRepositoryAdapter implements CheckInRepository {

    private final CheckInJpaRepository jpa;

    @Override
    public CheckIn save(CheckIn c) {
        var e = new CheckInJpaEntity();
        e.setGymId(c.gymId());
        e.setMemberId(c.memberId());
        e.setMembershipId(c.membershipId());
        e.setMethod(c.method());
        e.setResult(c.result());
        e.setReason(c.reason());
        e.setCheckedAt(c.checkedAt());
        e.setCheckedBy(c.checkedBy());
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public Optional<CheckIn> findLastAllowedSince(Long memberId, Instant since) {
        return jpa.findRecent(memberId, CheckIn.Result.ALLOWED, since, PageRequest.of(0, 1)).stream()
                .findFirst().map(CheckInRepositoryAdapter::toDomain);
    }

    @Override
    public PageResult<CheckIn> findBetween(Instant from, Instant to, int page, int size) {
        var result = jpa.findBetween(from, to, PageRequest.of(page, size));
        return new PageResult<>(result.getContent().stream().map(CheckInRepositoryAdapter::toDomain).toList(),
                page, size, result.getTotalElements());
    }

    @Override
    public long countAllowedBetween(Instant from, Instant to) {
        return jpa.countBetween(CheckIn.Result.ALLOWED, from, to);
    }

    private static CheckIn toDomain(CheckInJpaEntity e) {
        return new CheckIn(e.getId(), e.getGymId(), e.getMemberId(), e.getMembershipId(), e.getMethod(),
                e.getResult(), e.getReason(), e.getCheckedAt(), e.getCheckedBy());
    }
}
