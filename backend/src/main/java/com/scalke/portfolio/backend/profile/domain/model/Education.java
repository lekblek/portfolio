package com.scalke.portfolio.backend.profile.domain.model;

import java.util.Objects;

public record Education(
    Long id,
    String institution,
    String degree,
    String field,
    String location,
    DateRange period,
    String description,
    int displayOrder
) {

    public Education {
        Objects.requireNonNull(period, "period");
    }
}
