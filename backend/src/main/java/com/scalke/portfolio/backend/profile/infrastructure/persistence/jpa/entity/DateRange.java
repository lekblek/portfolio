package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public record DateRange(
    @Column(name = "start_date", nullable = false) LocalDate startDate,
    @Column(name = "end_date") LocalDate endDate
) {
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
