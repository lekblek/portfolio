package com.scalke.portfolio.backend.profile.web.dto;

import com.scalke.portfolio.backend.profile.domain.model.Skill;

/**
 * Compétence publique. Ni identifiant technique ni ordre d'affichage (D-R) : l'ordre du tableau fait foi.
 */
public record SkillResponse(String name) {

    static SkillResponse from(Skill skill) {
        return new SkillResponse(skill.name());
    }
}
