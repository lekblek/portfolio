package com.scalke.portfolio.backend.project.web.dto;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectStage;

import java.time.LocalDate;

/**
 * Détail public d'un projet ({@code GET /api/public/projects/{slug}}, D-W). Les champs optionnels
 * sont présents avec {@code null} (C10) ; la période est aplatie comme pour le parcours (D-P).
 */
public record ProjectResponse(
    String title,
    String slug,
    String shortDescription,
    String descriptionMarkdown,
    ProjectStage stage,
    LocalDate startDate,
    LocalDate endDate,
    String repositoryUrl,
    String demoUrl,
    boolean featured
) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
            project.title(),
            project.slug(),
            project.shortDescription(),
            project.descriptionMarkdown(),
            project.stage(),
            project.period().startDate(),
            project.period().endDate(),
            project.repositoryUrl(),
            project.demoUrl(),
            project.featured());
    }
}
