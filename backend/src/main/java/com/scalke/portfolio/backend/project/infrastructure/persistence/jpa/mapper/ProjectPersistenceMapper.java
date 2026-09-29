package com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectScreenshot;
import com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.entity.ProjectEntity;
import com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.entity.ProjectScreenshotEmbeddable;
import com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.entity.TechnologyEntity;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;
import com.scalke.portfolio.backend.shared.domain.model.Slug;

import java.util.List;

public final class ProjectPersistenceMapper {

    private ProjectPersistenceMapper() {
    }

    /**
     * Reconstruit le {@link Project} du domaine : ses invariants sont revérifiés à chaque lecture.
     * Parcourt les technologies et les captures : à appeler dans la transaction qui a chargé l'entité.
     */
    public static Project toDomain(ProjectEntity entity) {
        return new Project(
            entity.getId(),
            entity.getTitle(),
            Slug.of(entity.getSlug()),
            entity.getShortDescription(),
            entity.getDescriptionMarkdown(),
            entity.getStage(),
            entity.getVisibility(),
            new DateRange(entity.getStartDate(), entity.getEndDate()),
            entity.getRepositoryUrl(),
            entity.getDemoUrl(),
            entity.isFeatured(),
            entity.getDisplayOrder(),
            TechnologyPersistenceMapper.map(entity.getTechnologies()),
            entity.getCoverMediaId(),
            entity.getScreenshots().stream()
                .map(screenshot -> new ProjectScreenshot(
                    screenshot.getMediaId(), screenshot.getCaption(), screenshot.getDisplayOrder()))
                .toList(), entity.isEverPublished());
    }

    /**
     * Nouvelle entité, sans identifiant : attribué par PostgreSQL à l'insertion.
     *
     * @param technologies entités des technologies du projet, déjà persistées (fournies par l'adaptateur)
     */
    public static ProjectEntity toNewEntity(Project project, List<TechnologyEntity> technologies) {
        ProjectEntity entity = ProjectEntity.builder()
            .title(project.title())
            .slug(project.slug().value())
            .shortDescription(project.shortDescription())
            .descriptionMarkdown(project.descriptionMarkdown())
            .stage(project.stage())
            .visibility(project.visibility())
            .startDate(project.period().startDate())
            .endDate(project.period().endDate())
            .repositoryUrl(project.repositoryUrl())
            .demoUrl(project.demoUrl())
            .featured(project.featured())
            .displayOrder(project.displayOrder())
            .coverMediaId(project.coverMediaId())
            .screenshots(screenshots(project))
            .everPublished(project.everPublished())
            .build();
        technologies.forEach(entity::addTechnology);
        return entity;
    }

    public static List<ProjectScreenshotEmbeddable> screenshots(Project project) {
        return project.screenshots().stream()
            .map(screenshot -> new ProjectScreenshotEmbeddable(
                screenshot.mediaId(), screenshot.caption(), screenshot.displayOrder()))
            .toList();
    }
}
