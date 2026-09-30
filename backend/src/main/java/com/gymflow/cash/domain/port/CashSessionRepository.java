package com.gymflow.cash.domain.port;

import java.util.Optional;

import com.gymflow.cash.domain.model.CashSession;
import com.gymflow.shared.domain.model.PageResult;

/** Consultas filtradas por el gym actual (@TenantId). */
public interface CashSessionRepository {

    CashSession save(CashSession session);

    Optional<CashSession> findById(Long id);

    Optional<CashSession> findOpen();

    /** La caja abierta con SELECT ... FOR UPDATE: serializa ventas y cierre. Requiere transacción. */
    Optional<CashSession> lockOpen();

    /** Todas las cajas, la más reciente primero. */
    PageResult<CashSession> page(int page, int size);
}
