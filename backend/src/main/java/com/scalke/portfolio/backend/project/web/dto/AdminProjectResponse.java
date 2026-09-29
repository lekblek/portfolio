package com.scalke.portfolio.backend.project.web.dto;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectScreenshot;
import com.scalke.portfolio.backend.project.domain.model.ProjectStage;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.project.domain.model.Technology;

import java.time.LocalDate;
import java.util.List;

/**
 * Projet vu par l'administration (D-CX) : toute la saisie, technologies et médias par identifiant (technologies dans
 * l'ordre du vocabulaire, captures dans leur ordre d'affichage), et si le slug peut encore changer.
 */
public record AdminProjectResponse(
    Long id,
    String title,
    String slug,
    boolean slugLocked,
    String shortDescription,
    String descriptionMarkdown,
    ProjectStage stage,
    ProjectVisibility visibility,
    LocalDate startDate,
    LocalDate endDate,
    String repositoryUrl,
    String demoUrl,
    boolean featured,
    int displayOrder,
    List<Long> technologyIds,
    Long coverMediaId,
    List<Screenshot> screenshots
) {

    public static AdminProjectResponse from(Project project) {
        return new AdminProjectResponse(
            project.id(),
            project.title(),
            project.slug().value(),
            project.everPublished(),
            project.shortDescription(),
            project.descriptionMarkdown(),
            project.stage(),
            project.visibility(),
            project.period().startDate(),
            project.period().endDate(),
            project.repositoryUrl(),
            project.demoUrl(),
            project.featured(),
            project.displayOrder(),
            project.technologies().stream().map(Technology::id).toList(),
            project.coverMediaId(),
            project.screenshots().stream().map(Screenshot::from).toList());
    }

    public record Screenshot(Long mediaId, String caption) {

        static Screenshot from(ProjectScreenshot screenshot) {
            return new Screenshot(screenshot.mediaId(), screenshot.caption());
        }
    }
}
