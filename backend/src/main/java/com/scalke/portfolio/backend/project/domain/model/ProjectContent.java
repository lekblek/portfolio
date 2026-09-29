package com.scalke.portfolio.backend.project.domain.model;

import com.scalke.portfolio.backend.shared.domain.model.DateRange;

import java.util.List;

/**
 * Ce que l'administrateur saisit d'un projet (D-CX) : tout sauf le slug (règle propre, {@link Project#edit}) et la
 * mémoire de publication. Technologies et captures déjà résolues (technologies existantes, captures ordonnées). Les
 * invariants sont vérifiés par {@link Project}.
 */
public record ProjectContent(
    String title,
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
}
