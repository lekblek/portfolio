package com.scalke.portfolio.backend.profile.domain.model;

import com.scalke.portfolio.backend.shared.domain.model.DateRange;

import java.util.Objects;

public record Experience(
    Long id,
    String organization,
    String title,
    String location,
    DateRange period,
    String description,
    int displayOrder
) {

    public Experience {
        Objects.requireNonNull(period, "period");
    }
}
