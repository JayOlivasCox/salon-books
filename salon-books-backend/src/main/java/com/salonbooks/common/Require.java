package com.salonbooks.common;

import java.math.BigDecimal;

/** Field checks used inside SyncDto.applyTo (delete-only payloads skip these). */
public final class Require {
    private Require() {
    }

    public static <T> T notNull(T value, String field) {
        if (value == null) throw ApiException.badRequest(field + " is required");
        return value;
    }

    public static String notBlank(String value, String field) {
        if (value == null || value.isBlank()) throw ApiException.badRequest(field + " is required");
        return value.trim();
    }

    public static BigDecimal nonNegative(BigDecimal value, String field) {
        notNull(value, field);
        if (value.signum() < 0) throw ApiException.badRequest(field + " cannot be negative");
        return value;
    }

    public static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
