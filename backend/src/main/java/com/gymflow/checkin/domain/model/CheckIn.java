package com.gymflow.checkin.domain.model;

import java.time.Instant;

/** Un intento de entrada. reason es null si se permitió; memberId es null si el código no era de ningún socio. */
public record CheckIn(
        Long id,
        Long gymId,
        Long memberId,
        Long membershipId,
        Method method,
        Result result,
        DenyReason reason,
        Instant checkedAt,
        Long checkedBy) {

    public enum Method { QR, DNI }

    public enum Result { ALLOWED, DENIED }

    public static CheckIn allowed(Long gymId, Long memberId, Long membershipId, Method method, Instant now, Long by) {
        return new CheckIn(null, gymId, memberId, membershipId, method, Result.ALLOWED, null, now, by);
    }

    public static CheckIn denied(Long gymId, Long memberId, Long membershipId, Method method, DenyReason reason,
                                 Instant now, Long by) {
        return new CheckIn(null, gymId, memberId, membershipId, method, Result.DENIED, reason, now, by);
    }
}
