package com.scalke.portfolio.backend.project.web.dto;

import com.scalke.portfolio.backend.project.domain.model.Technology;

/**
 * Technologie vue par l'administration (D-CW) : avec son identifiant, que les projets référencent.
 */
public record AdminTechnologyResponse(Long id, String name, String slug, int displayOrder) {

    public static AdminTechnologyResponse from(Technology technology) {
        return new AdminTechnologyResponse(technology.id(), technology.name(), technology.slug().value(),
            technology.displayOrder());
    }
}
