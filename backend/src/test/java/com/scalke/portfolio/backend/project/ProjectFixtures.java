package com.scalke.portfolio.backend.project;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectStage;
import com.scalke.portfolio.backend.project.domain.model.ProjectVisibility;
import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;

import java.time.LocalDate;
import java.util.List;

/**
 * Projets du domaine pour les tests. Seuls le slug, la visibilité, la période, l'ordre et les
 * technologies varient ; l'état métier est déduit de la période pour respecter l'invariant 21.
 */
public final class ProjectFixtures {

    private ProjectFixtures() {
    }

    public static Project project(String slug, ProjectVisibility visibility, DateRange period, int displayOrder,
                                  Technology... technologies) {
        return new Project(
            null,
            "Projet " + slug,
            slug,
            "Résumé de " + slug,
            "# " + slug,
            period.isOngoing() ? ProjectStage.IN_PROGRESS : ProjectStage.COMPLETED,
            visibility,
            period,
            null,
            null,
            false,
            displayOrder,
            List.of(technologies));
    }

    public static Project published(String slug, LocalDate start, int displayOrder, Technology... technologies) {
        return project(slug, ProjectVisibility.PUBLISHED, DateRange.ongoingSince(start), displayOrder, technologies);
    }

    /**
     * Technologie non encore persistée : à créer par {@code TechnologyRepository.create} avant de
     * l'associer à un projet persisté.
     */
    public static Technology technology(String name, String slug, int displayOrder) {
        return new Technology(null, name, slug, displayOrder);
    }
}
