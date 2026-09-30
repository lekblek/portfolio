package com.scalke.portfolio.backend.project.web.dto;

import com.scalke.portfolio.backend.media.application.query.PublicImage;
import com.scalke.portfolio.backend.project.application.usecase.PublishedProject;
import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectStage;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;

/**
 * Détail public d'un projet ({@code GET /api/public/projects/{slug}}, D-W). Les champs optionnels
 * sont présents avec {@code null} (C10) ; la période est aplatie comme pour le parcours (D-P). Couverture
 * ({@code null} si aucune) et captures ({@code []} si aucune) sous leur forme publique (D-BW).
 */
public record ProjectResponse(
    String title,
    String slug,
    String shortDescription,
    String descriptionMarkdown,
    ProjectStage stage,
    LocalDate startDate,
    @Nullable LocalDate endDate,
    @Nullable String repositoryUrl,
    @Nullable String demoUrl,
    boolean featured,
    @Nullable PublicImage cover,
    List<ProjectScreenshotResponse> screenshots,
    List<TechnologyResponse> technologies
) {

    public static ProjectResponse from(PublishedProject published) {
        Project project = published.project();
        return new ProjectResponse(
            project.title(),
            project.slug().value(),
            project.shortDescription(),
            project.descriptionMarkdown(),
            project.stage(),
            project.period().startDate(),
            project.period().endDate(),
            project.repositoryUrl(),
            project.demoUrl(),
            project.featured(),
            published.cover(),
            ProjectScreenshotResponse.from(published.screenshots()),
            TechnologyResponse.from(project.technologies()));
    }
}
