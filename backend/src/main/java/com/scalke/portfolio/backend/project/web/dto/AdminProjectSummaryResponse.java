package com.scalke.portfolio.backend.project.web.dto;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectStage;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;

/**
 * Ligne de la liste d'administration des projets (D-CX).
 */
public record AdminProjectSummaryResponse(
    Long id,
    String title,
    String slug,
    ProjectVisibility visibility,
    ProjectStage stage,
    boolean featured,
    int displayOrder
) {

    public static AdminProjectSummaryResponse from(Project project) {
        return new AdminProjectSummaryResponse(project.id(), project.title(), project.slug().value(),
            project.visibility(), project.stage(), project.featured(), project.displayOrder());
    }
}
