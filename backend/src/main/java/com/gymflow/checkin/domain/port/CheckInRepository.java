package com.gymflow.checkin.domain.port;

import java.time.Instant;
import java.util.Optional;

import com.gymflow.checkin.domain.model.CheckIn;
import com.gymflow.shared.domain.model.PageResult;

/** Consultas filtradas por el gym actual (@TenantId). */
public interface CheckInRepository {

    CheckIn save(CheckIn checkIn);

    /** Última entrada permitida del socio desde `since` (para no contar dos veces la misma visita). */
    Optional<CheckIn> findLastAllowedSince(Long memberId, Instant since);

    /** Intentos (permitidos y denegados) en [from, to), el más reciente primero. */
    PageResult<CheckIn> findBetween(Instant from, Instant to, int page, int size);

    long countAllowedBetween(Instant from, Instant to);
}
