package com.scalke.portfolio.backend.profile.domain.model;

public record Skill(
    Long id,
    String name,
    String category,
    Integer displayOrder
) {
}
