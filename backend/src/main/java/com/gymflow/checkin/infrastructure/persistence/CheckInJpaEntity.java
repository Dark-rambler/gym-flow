package com.gymflow.checkin.infrastructure.persistence;

import java.time.Instant;

import com.gymflow.checkin.domain.model.CheckIn;
import com.gymflow.checkin.domain.model.DenyReason;
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

// Registro inmutable: se inserta y nunca se modifica.
@Entity
@Table(name = "check_in")
@Getter
@Setter
@NoArgsConstructor
public class CheckInJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "gym_id", nullable = false, updatable = false)
    private Long gymId;

    @Column(name = "member_id", updatable = false)
    private Long memberId;

    @Column(name = "membership_id", updatable = false)
    private Long membershipId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5, updatable = false)
    private CheckIn.Method method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private CheckIn.Result result;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, updatable = false)
    private DenyReason reason;

    @Column(name = "checked_at", nullable = false, updatable = false)
    private Instant checkedAt;

    @Column(name = "checked_by", nullable = false, updatable = false)
    private Long checkedBy;
}
