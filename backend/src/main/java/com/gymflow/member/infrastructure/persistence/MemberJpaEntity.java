package com.gymflow.member.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "member")
@Getter
@Setter
@NoArgsConstructor
public class MemberJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "gym_id", nullable = false, updatable = false)
    private Long gymId;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 12)
    private String dni;

    @Column(length = 20)
    private String phone;

    @Column(length = 160)
    private String email;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 500)
    private String notes;

    @Column(name = "qr_token", nullable = false, updatable = false, unique = true)
    private UUID qrToken;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
