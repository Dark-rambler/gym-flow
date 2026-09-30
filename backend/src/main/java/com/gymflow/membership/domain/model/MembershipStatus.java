package com.gymflow.membership.domain.model;

/**
 * Estado efectivo que ve el usuario. En BD solo se guardan ACTIVE, FROZEN y CANCELLED;
 * SCHEDULED y EXPIRED se derivan de las fechas (ver Membership.statusOn).
 */
public enum MembershipStatus {
    SCHEDULED,
    ACTIVE,
    FROZEN,
    EXPIRED,
    CANCELLED
}
