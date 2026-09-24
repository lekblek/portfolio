package com.scalke.portfolio.backend.profile.web.dto;

import com.scalke.portfolio.backend.profile.domain.model.Skill;

public record SkillResponse(
    Long id,
    String name,
    Integer displayOrder
) {

    static SkillResponse from(Skill skill) {
        return new SkillResponse(
            skill.id(),
            skill.name(),
            skill.displayOrder()
        );
    }
}
