package com.scalke.portfolio.backend.taxonomy.domain.model;

import com.scalke.portfolio.backend.shared.domain.model.Slug;

import java.util.Objects;

/**
 * Catégorie éditoriale (racine d'agrégat). Une publication en possède au plus une (D20).
 * <p>
 * Invariant 25 : nom (sans tenir compte de la casse) et slug uniques ; une catégorie utilisée par une
 * publication ne peut pas être supprimée. Garanti par PostgreSQL ({@code V007}, {@code V008}, D-AO).
 */
public record Category(Long id, String name, Slug slug, String description) {

    public Category {
        Objects.requireNonNull(slug, "slug");
    }
}
