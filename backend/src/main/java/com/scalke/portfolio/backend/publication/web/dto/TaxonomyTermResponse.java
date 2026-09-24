package com.scalke.portfolio.backend.publication.web.dto;

import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;

import java.util.List;

/**
 * Catégorie ou tag d'une publication publique : nom affiché et slug utilisable dans {@code ?category=} /
 * {@code ?tag=} (D-AP). Ni identifiant ni description.
 */
public record TaxonomyTermResponse(String name, String slug) {

    static TaxonomyTermResponse from(Category category) {
        return category == null ? null : new TaxonomyTermResponse(category.name(), category.slug());
    }

    static List<TaxonomyTermResponse> from(List<Tag> tags) {
        return tags.stream().map(tag -> new TaxonomyTermResponse(tag.name(), tag.slug())).toList();
    }
}
