package com.scalke.portfolio.backend.project.domain.model;

import java.util.Comparator;

/**
 * Technologie du vocabulaire des projets (racine d'agrégat, distincte des tags éditoriaux : D21).
 * <p>
 * Invariant 22 : nom (insensible à la casse) et slug uniques ; une technologie utilisée par un projet
 * ne peut pas être supprimée. Garanti par PostgreSQL ({@code V005}), comme le format du slug (D-AA).
 */
public record Technology(Long id, String name, String slug, int displayOrder) {

    /**
     * Ordre d'affichage du vocabulaire, total : {@code displayOrder}, puis nom, puis slug (unique).
     */
    public static final Comparator<Technology> DISPLAY_ORDER = Comparator
        .comparingInt(Technology::displayOrder)
        .thenComparing(Technology::name, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(Technology::slug);
}
