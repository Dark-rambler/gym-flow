package com.gymflow.membership.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.gymflow.membership.domain.model.MembershipStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "membership")
@Getter
@Setter
@NoArgsConstructor
public class MembershipJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "gym_id", nullable = false, updatable = false)
    private Long gymId;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "plan_id", nullable = false, updatable = false)
    private Long planId;

    @Column(name = "plan_name", nullable = false, length = 80, updatable = false)
    private String planName;

    @Column(nullable = false, precision = 10, scale = 2, updatable = false)
    private BigDecimal price;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // solo ACTIVE | FROZEN | CANCELLED (CHECK en V3)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private MembershipStatus status;

    @Column(name = "frozen_since")
    private LocalDate frozenSince;

    @Column(name = "frozen_days", nullable = false)
    private int frozenDays;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
