package com.scalke.portfolio.backend.profile.web.dto;

import com.scalke.portfolio.backend.profile.domain.model.Education;

import java.time.LocalDate;

/**
 * Formation publique. La période est aplatie (D-P) : {@code endDate == null} signifie « en cours ».
 */
public record EducationResponse(
    String institution,
    String degree,
    String field,
    String location,
    LocalDate startDate,
    LocalDate endDate,
    String description
) {

    static EducationResponse from(Education education) {
        return new EducationResponse(
            education.institution(),
            education.degree(),
            education.field(),
            education.location(),
            education.period().startDate(),
            education.period().endDate(),
            education.description());
    }
}
