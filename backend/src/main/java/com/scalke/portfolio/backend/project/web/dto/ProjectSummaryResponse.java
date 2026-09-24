package com.scalke.portfolio.backend.project.web.dto;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectStage;

import java.time.LocalDate;
import java.util.List;

/**
 * Projet dans une liste publique (D-W). Ni identifiant, ni {@code displayOrder}, ni visibilité :
 * l'ordre du tableau fait foi et tout projet listé est publié. {@code endDate == null} : en cours.
 * {@code technologies} vaut {@code []} lorsqu'il n'y en a aucune.
 */
public record ProjectSummaryResponse(
    String title,
    String slug,
    String shortDescription,
    ProjectStage stage,
    LocalDate startDate,
    LocalDate endDate,
    boolean featured,
    List<TechnologyResponse> technologies
) {

    public static ProjectSummaryResponse from(Project project) {
        return new ProjectSummaryResponse(
            project.title(),
            project.slug(),
            project.shortDescription(),
            project.stage(),
            project.period().startDate(),
            project.period().endDate(),
            project.featured(),
            TechnologyResponse.from(project.technologies()));
    }
}
