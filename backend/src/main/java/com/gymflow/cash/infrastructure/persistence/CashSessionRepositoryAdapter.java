package com.gymflow.cash.infrastructure.persistence;

import java.util.Optional;

import com.gymflow.cash.domain.model.CashSession;
import com.gymflow.cash.domain.port.CashSessionRepository;
import com.gymflow.shared.domain.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class CashSessionRepositoryAdapter implements CashSessionRepository {

    private final CashSessionJpaRepository jpa;

    @Override
    public CashSession save(CashSession s) {
        var e = new CashSessionJpaEntity();
        e.setId(s.id());
        e.setGymId(s.gymId());
        e.setStatus(s.status());
        e.setOpenedBy(s.openedBy());
        e.setOpenedAt(s.openedAt());
        e.setOpeningAmount(s.openingAmount());
        e.setClosedBy(s.closedBy());
        e.setClosedAt(s.closedAt());
        e.setCountedCash(s.countedCash());
        e.setExpectedCash(s.expectedCash());
        e.setDifference(s.difference());
        e.setNotes(s.notes());
        // flush inmediato: uk_cash_session_open sale aquí y no al commit
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    public Optional<CashSession> findById(Long id) {
        return jpa.findScopedById(id).map(CashSessionRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<CashSession> findOpen() {
        return jpa.findOpen().map(CashSessionRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<CashSession> lockOpen() {
        return jpa.lockOpen().map(CashSessionRepositoryAdapter::toDomain);
    }

    @Override
    public PageResult<CashSession> page(int page, int size) {
        var result = jpa.findAllSorted(PageRequest.of(page, size));
        return new PageResult<>(result.getContent().stream().map(CashSessionRepositoryAdapter::toDomain).toList(),
                page, size, result.getTotalElements());
    }

    private static CashSession toDomain(CashSessionJpaEntity e) {
        return new CashSession(e.getId(), e.getGymId(), e.getStatus(), e.getOpenedBy(), e.getOpenedAt(),
                e.getOpeningAmount(), e.getClosedBy(), e.getClosedAt(), e.getCountedCash(), e.getExpectedCash(),
                e.getDifference(), e.getNotes());
    }
}
