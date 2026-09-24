package com.scalke.portfolio.backend.profile.domain.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Période d'une expérience ou d'une formation. {@code endDate == null} signifie « en cours ».
 * <p>
 * Invariant (D-I, D-N) : la fin n'est jamais antérieure au début. Doublé par les contraintes
 * {@code experience_dates_check} et {@code education_dates_check} de {@code V003}.
 */
public record DateRange(LocalDate startDate, LocalDate endDate) {

    public DateRange {
        Objects.requireNonNull(startDate, "startDate");
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
    }

    public static DateRange between(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, Objects.requireNonNull(endDate, "endDate"));
    }

    public static DateRange ongoingSince(LocalDate startDate) {
        return new DateRange(startDate, null);
    }

    public boolean isOngoing() {
        return endDate == null;
    }
}
