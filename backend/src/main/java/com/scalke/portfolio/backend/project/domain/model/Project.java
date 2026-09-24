package com.scalke.portfolio.backend.project.domain.model;

import com.scalke.portfolio.backend.shared.domain.model.DateRange;

import java.util.Objects;

/**
 * Projet du portfolio (racine d'agrégat). Technologies, captures et couverture arrivent aux
 * étapes 18 et 27 (D-Y).
 * <p>
 * Invariants :
 * <ul>
 *   <li>la période ne se termine jamais avant de commencer ({@link DateRange}, invariant 17) ;</li>
 *   <li>{@code stage} est cohérent avec la période : {@code IN_PROGRESS} si et seulement si
 *       {@code period.isOngoing()} (invariant 21, D-T). Doublé par {@code project_stage_matches_dates_check}.</li>
 * </ul>
 * Le slug est unique et au format kebab-case, garanti par PostgreSQL (D-X) ; sa génération et la
 * gestion des collisions arrivent à l'étape 22.
 */
public record Project(
    Long id,
    String title,
    String slug,
    String shortDescription,
    String descriptionMarkdown,
    ProjectStage stage,
    ProjectVisibility visibility,
    DateRange period,
    String repositoryUrl,
    String demoUrl,
    boolean featured,
    int displayOrder
) {

    public Project {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(visibility, "visibility");
        Objects.requireNonNull(period, "period");
        if ((stage == ProjectStage.IN_PROGRESS) != period.isOngoing()) {
            throw new IllegalArgumentException(
                "stage " + stage + " contradicts the period " + period + ": IN_PROGRESS requires no end date");
        }
    }
}
