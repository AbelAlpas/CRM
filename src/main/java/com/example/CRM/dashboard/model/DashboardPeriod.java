package com.example.CRM.dashboard.model;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Locale;

public enum DashboardPeriod {
    MONTH("Este mes"),
    LAST_30_DAYS("Últimos 30 días"),
    YEAR("Este año"),
    ALL("Todo");

    private final String label;

    DashboardPeriod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static DashboardPeriod fromParam(String value) {
        if (value == null || value.isBlank()) return MONTH;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return MONTH;
        }
    }

    public PeriodBounds bounds(ZonedDateTime now) {
        ZonedDateTime start = switch (this) {
            case MONTH -> now.withDayOfMonth(1).toLocalDate().atStartOfDay(now.getZone());
            case LAST_30_DAYS -> now.minusDays(30);
            case YEAR -> now.withDayOfYear(1).toLocalDate().atStartOfDay(now.getZone());
            case ALL -> now.withYear(1900).withDayOfYear(1).toLocalDate().atStartOfDay(now.getZone());
        };
        return new PeriodBounds(start.toOffsetDateTime(), now.toOffsetDateTime(), this == ALL);
    }

    public record PeriodBounds(OffsetDateTime from, OffsetDateTime to, boolean all) { }
}
