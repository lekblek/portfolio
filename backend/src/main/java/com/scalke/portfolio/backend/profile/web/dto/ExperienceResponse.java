package com.scalke.portfolio.backend.profile.web.dto;

import com.scalke.portfolio.backend.profile.domain.model.Experience;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

/**
 * Expérience publique. La période est aplatie (D-P) : {@code endDate == null} signifie « en cours ».
 */
public record ExperienceResponse(
    String organization,
    String title,
    String location,
    LocalDate startDate,
    @Nullable LocalDate endDate,
    String description
) {

    static ExperienceResponse from(Experience experience) {
        return new ExperienceResponse(
            experience.organization(),
            experience.title(),
            experience.location(),
            experience.period().startDate(),
            experience.period().endDate(),
            experience.description());
    }
}
