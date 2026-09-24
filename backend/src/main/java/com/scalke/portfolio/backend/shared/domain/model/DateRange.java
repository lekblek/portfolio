package com.scalke.portfolio.backend.shared.domain.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Période datée. {@code endDate == null} signifie « en cours ».
 * <p>
 * Invariant (D-I, D-N) : la fin n'est jamais antérieure au début. Doublé par une contrainte
 * {@code CHECK} dans chaque table qui stocke une période ({@code experience}, {@code education},
 * {@code project}).
 * <p>
 * Partagé depuis l'étape 17 (D-S) : utilisé par {@code profile} et {@code project}, qui ne peuvent
 * pas dépendre l'un de l'autre.
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
