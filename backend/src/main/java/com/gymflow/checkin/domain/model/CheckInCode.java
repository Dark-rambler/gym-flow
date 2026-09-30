package com.gymflow.checkin.domain.model;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Lo que llega del campo de recepción: un QR (payload "GF1:&lt;uuid&gt;" o el uuid solo) o un DNI tecleado.
 * Un lector USB escribe el contenido del QR como si fuera teclado, por eso ambos llegan por el mismo campo.
 */
public sealed interface CheckInCode {

    String QR_PREFIX = "GF1:";

    record Qr(UUID token) implements CheckInCode {
    }

    record Dni(String value) implements CheckInCode {
    }

    record Invalid() implements CheckInCode {
    }

    Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    Pattern DNI_PATTERN = Pattern.compile("^[A-Z0-9]{6,12}$");

    static CheckInCode parse(String raw) {
        if (raw == null || raw.isBlank() || raw.length() > 100) {
            return new Invalid();
        }
        String s = raw.trim();
        if (s.regionMatches(true, 0, QR_PREFIX, 0, QR_PREFIX.length())) {
            String rest = s.substring(QR_PREFIX.length()).trim();
            return UUID_PATTERN.matcher(rest).matches() ? new Qr(UUID.fromString(rest)) : new Invalid();
        }
        if (UUID_PATTERN.matcher(s).matches()) {
            return new Qr(UUID.fromString(s));
        }
        // misma normalización que Member.normalizeDni: sin espacios, en mayúsculas
        String dni = s.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        return DNI_PATTERN.matcher(dni).matches() ? new Dni(dni) : new Invalid();
    }

    static String payloadFor(UUID token) {
        return QR_PREFIX + token;
    }
}
