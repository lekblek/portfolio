package com.scalke.portfolio.backend.project.domain.model;

import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import com.scalke.portfolio.backend.shared.domain.model.Slug;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Projet du portfolio (racine d'agrégat).
 * <p>
 * Invariants :
 * <ul>
 *   <li>la période ne se termine jamais avant de commencer ({@link DateRange}, invariant 17) ;</li>
 *   <li>{@code stage} est cohérent avec la période : {@code IN_PROGRESS} si et seulement si
 *       {@code period.isOngoing()} (invariant 21, D-T). Doublé par {@code project_stage_matches_dates_check} ;</li>
 *   <li>une technologie apparaît au plus une fois (invariant 23). Doublé par {@code project_technology_pk} ;</li>
 *   <li>une capture apparaît au plus une fois (invariant 27). Doublé par {@code project_screenshot_pk}.</li>
 * </ul>
 * Couverture ({@code coverMediaId}, facultative) et captures sont des références vers le catalogue
 * {@code media}, par identifiant (ADR 0002, D-BV) ; PostgreSQL interdit de supprimer un média ainsi référencé
 * (invariant 13). Les captures sont toujours rangées dans leur ordre d'affichage.
 * Les technologies sont des références vers un autre agrégat, toujours rangées dans l'ordre du
 * vocabulaire ({@link Technology#DISPLAY_ORDER}, D-Z). Le slug est un {@link Slug} (format vérifié par le
 * domaine, D-BB), unique par PostgreSQL (D-X). Sa stabilité après publication (D11) sera appliquée avec
 * la gestion de la visibilité des projets (administration, étape 36).
 */
public record Project(
    Long id,
    String title,
    Slug slug,
    String shortDescription,
    String descriptionMarkdown,
    ProjectStage stage,
    ProjectVisibility visibility,
    DateRange period,
    String repositoryUrl,
    String demoUrl,
    boolean featured,
    int displayOrder,
    List<Technology> technologies,
    Long coverMediaId,
    List<ProjectScreenshot> screenshots
) {

    public Project {
        Objects.requireNonNull(slug, "slug");
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(visibility, "visibility");
        Objects.requireNonNull(period, "period");
        if ((stage == ProjectStage.IN_PROGRESS) != period.isOngoing()) {
            throw new IllegalArgumentException(
                "stage " + stage + " contradicts the period " + period + ": IN_PROGRESS requires no end date");
        }
        technologies = Objects.requireNonNull(technologies, "technologies").stream()
            .sorted(Technology.DISPLAY_ORDER)
            .toList();
        Set<Slug> slugs = new HashSet<>();
        for (Technology technology : technologies) {
            if (!slugs.add(technology.slug())) {
                throw new IllegalArgumentException("technology " + technology.slug() + " appears twice");
            }
        }
        screenshots = Objects.requireNonNull(screenshots, "screenshots").stream()
            .sorted(ProjectScreenshot.DISPLAY_ORDER)
            .toList();
        Set<Long> media = new HashSet<>();
        for (ProjectScreenshot screenshot : screenshots) {
            if (!media.add(screenshot.mediaId())) {
                throw new IllegalArgumentException("screenshot " + screenshot.mediaId() + " appears twice");
            }
        }
    }
}
