package com.gymflow.member.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

// Socio del gimnasio. El DNI es único por gym y se guarda normalizado (sin espacios, en mayúsculas).
public record Member(
        Long id,
        Long gymId,
        String fullName,
        String dni,
        String phone,
        String email,
        LocalDate birthDate,
        String notes,
        UUID qrToken,
        boolean active,
        Instant createdAt) {

    public static Member create(Long gymId, MemberData data) {
        return new Member(null, gymId, data.fullName().trim(), normalizeDni(data.dni()), blankToNull(data.phone()),
                blankToNull(data.email()), data.birthDate(), blankToNull(data.notes()), UUID.randomUUID(), true, null);
    }

    public Member update(MemberData data) {
        return new Member(id, gymId, data.fullName().trim(), normalizeDni(data.dni()), blankToNull(data.phone()),
                blankToNull(data.email()), data.birthDate(), blankToNull(data.notes()), qrToken, active, createdAt);
    }

    /** Nuevo QR: el carnet impreso y el enlace público anteriores dejan de valer. */
    public Member rotateQr() {
        return new Member(id, gymId, fullName, dni, phone, email, birthDate, notes, UUID.randomUUID(), active, createdAt);
    }

    public Member withActive(boolean newActive) {
        return new Member(id, gymId, fullName, dni, phone, email, birthDate, notes, qrToken, newActive, createdAt);
    }

    public static String normalizeDni(String dni) {
        return dni.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Datos editables del socio. */
    public record MemberData(String fullName, String dni, String phone, String email, LocalDate birthDate, String notes) {
    }
}
