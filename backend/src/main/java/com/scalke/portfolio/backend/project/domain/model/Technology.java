package com.scalke.portfolio.backend.project.domain.model;

import com.scalke.portfolio.backend.shared.domain.model.Slug;

import java.util.Comparator;
import java.util.Objects;

/**
 * Technologie du vocabulaire des projets (racine d'agrégat, distincte des tags éditoriaux : D21).
 * <p>
 * Invariant 22 : nom (insensible à la casse) et slug uniques ; une technologie utilisée par un projet
 * ne peut pas être supprimée. Garanti par PostgreSQL ({@code V005}), comme le format du slug (D-AA).
 * <p>
 * Bornes (D-CW), celles des colonnes : nom non vide de 80 caractères au plus, slug de 80 ; ordre d'affichage positif
 * ou nul.
 */
public record Technology(Long id, String name, Slug slug, int displayOrder) {

    public static final int NAME_MAX_LENGTH = 80;
    public static final int SLUG_MAX_LENGTH = 80;

    /**
     * Ordre d'affichage du vocabulaire, total : {@code displayOrder}, puis nom, puis slug (unique).
     */
    public static final Comparator<Technology> DISPLAY_ORDER = Comparator
        .comparingInt(Technology::displayOrder)
        .thenComparing(Technology::name, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(technology -> technology.slug().value());

    public Technology {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(slug, "slug");
        if (name.isBlank() || name.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("name must be 1 to 80 characters");
        }
        if (slug.value().length() > SLUG_MAX_LENGTH) {
            throw new IllegalArgumentException("slug must be at most 80 characters");
        }
        if (displayOrder < 0) {
            throw new IllegalArgumentException("display order must not be negative");
        }
    }
}
