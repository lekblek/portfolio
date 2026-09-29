package com.scalke.portfolio.backend.taxonomy.web.dto;

import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;

/**
 * Tag vu par l'administration (D-CS) : avec son identifiant, que les routes d'administration utilisent.
 */
public record AdminTagResponse(Long id, String name, String slug) {

    public static AdminTagResponse from(Tag tag) {
        return new AdminTagResponse(tag.id(), tag.name(), tag.slug().value());
    }
}
