package com.scalke.portfolio.backend.taxonomy.web.dto;

import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import org.jspecify.annotations.Nullable;

/**
 * Catégorie vue par l'administration (D-CS) : avec son identifiant, que les routes d'administration utilisent.
 */
public record AdminCategoryResponse(Long id, String name, String slug, @Nullable String description) {

    public static AdminCategoryResponse from(Category category) {
        return new AdminCategoryResponse(category.id(), category.name(), category.slug().value(), category.description());
    }
}
