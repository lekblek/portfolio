package com.scalke.portfolio.backend.taxonomy.domain.model;

import com.scalke.portfolio.backend.shared.domain.model.Slug;

import java.util.Objects;

/**
 * Règles communes aux termes d'un vocabulaire (catégories, tags) : nom non blanc et borné, slug borné par sa colonne.
 */
final class Terms {

    private Terms() {
    }

    static void requireName(String name, int maxLength) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank() || name.length() > maxLength) {
            throw new IllegalArgumentException("name must be 1 to " + maxLength + " characters and not blank");
        }
    }

    static void requireSlug(Slug slug, int maxLength) {
        Objects.requireNonNull(slug, "slug");
        if (slug.value().length() > maxLength) {
            throw new IllegalArgumentException("slug must not exceed " + maxLength + " characters");
        }
    }
}
