package com.scalke.portfolio.backend.profile.domain.model;

public record ProfessionalLink(
    Long id,
    String label,
    String url,
    Integer displayOrder
) {
}
