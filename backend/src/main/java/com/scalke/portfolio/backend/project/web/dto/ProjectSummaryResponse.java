package com.scalke.portfolio.backend.project.web.dto;

import com.scalke.portfolio.backend.media.application.query.PublicImage;
import com.scalke.portfolio.backend.project.application.usecase.PublishedProject;
import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectStage;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;

/**
 * Projet dans une liste publique (D-W). Ni identifiant, ni {@code displayOrder}, ni visibilité :
 * l'ordre du tableau fait foi et tout projet listé est publié. {@code endDate == null} : en cours.
 * {@code technologies} vaut {@code []} lorsqu'il n'y en a aucune ; {@code cover} vaut {@code null} sans couverture
 * (D-BW).
 */
public record ProjectSummaryResponse(
    String title,
    String slug,
    String shortDescription,
    ProjectStage stage,
    LocalDate startDate,
    @Nullable LocalDate endDate,
    boolean featured,
    @Nullable PublicImage cover,
    List<TechnologyResponse> technologies
) {

    public static ProjectSummaryResponse from(PublishedProject published) {
        Project project = published.project();
        return new ProjectSummaryResponse(
            project.title(),
            project.slug().value(),
            project.shortDescription(),
            project.stage(),
            project.period().startDate(),
            project.period().endDate(),
            project.featured(),
            published.cover(),
            TechnologyResponse.from(project.technologies()));
    }
}
