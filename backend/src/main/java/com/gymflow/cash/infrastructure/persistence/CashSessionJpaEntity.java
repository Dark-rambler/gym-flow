package com.gymflow.cash.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;

import com.gymflow.cash.domain.model.CashSession;
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
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "cash_session")
@Getter
@Setter
@NoArgsConstructor
public class CashSessionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "gym_id", nullable = false, updatable = false)
    private Long gymId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CashSession.Status status;

    @Column(name = "opened_by", nullable = false, updatable = false)
    private Long openedBy;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "opening_amount", nullable = false, precision = 10, scale = 2, updatable = false)
    private BigDecimal openingAmount;

    @Column(name = "closed_by")
    private Long closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "counted_cash", precision = 10, scale = 2)
    private BigDecimal countedCash;

    @Column(name = "expected_cash", precision = 10, scale = 2)
    private BigDecimal expectedCash;

    @Column(precision = 10, scale = 2)
    private BigDecimal difference;

    @Column(length = 500)
    private String notes;
}
