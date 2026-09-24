package com.scalke.portfolio.backend.project.web.dto;

import com.scalke.portfolio.backend.project.domain.model.Technology;

import java.util.List;

/**
 * Technologie publique : nom affiché et slug utilisable dans {@code ?technology=} (D-AC).
 * Ni identifiant ni {@code displayOrder} (D-R) : l'ordre du tableau fait foi.
 */
public record TechnologyResponse(String name, String slug) {

    static TechnologyResponse from(Technology technology) {
        return new TechnologyResponse(technology.name(), technology.slug());
    }

    static List<TechnologyResponse> from(List<Technology> technologies) {
        return technologies.stream().map(TechnologyResponse::from).toList();
    }
}
