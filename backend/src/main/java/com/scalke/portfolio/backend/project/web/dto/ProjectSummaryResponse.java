package com.scalke.portfolio.backend.project.web.dto;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectStage;

import java.time.LocalDate;

/**
 * Projet dans une liste publique (D-W). Ni identifiant, ni {@code displayOrder}, ni visibilité :
 * l'ordre du tableau fait foi et tout projet listé est publié. {@code endDate == null} : en cours.
 */
public record ProjectSummaryResponse(
    String title,
    String slug,
    String shortDescription,
    ProjectStage stage,
    LocalDate startDate,
    LocalDate endDate,
    boolean featured
) {

    public static ProjectSummaryResponse from(Project project) {
        return new ProjectSummaryResponse(
            project.title(),
            project.slug(),
            project.shortDescription(),
            project.stage(),
            project.period().startDate(),
            project.period().endDate(),
            project.featured());
    }
}
