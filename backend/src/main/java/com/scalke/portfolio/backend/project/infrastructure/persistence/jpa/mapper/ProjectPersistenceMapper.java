package com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.entity.ProjectEntity;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;

public final class ProjectPersistenceMapper {

    private ProjectPersistenceMapper() {
    }

    /**
     * Reconstruit le {@link Project} du domaine : ses invariants sont revérifiés à chaque lecture.
     */
    public static Project toDomain(ProjectEntity entity) {
        return new Project(
            entity.getId(),
            entity.getTitle(),
            entity.getSlug(),
            entity.getShortDescription(),
            entity.getDescriptionMarkdown(),
            entity.getStage(),
            entity.getVisibility(),
            new DateRange(entity.getStartDate(), entity.getEndDate()),
            entity.getRepositoryUrl(),
            entity.getDemoUrl(),
            entity.isFeatured(),
            entity.getDisplayOrder()
        );
    }

    /**
     * Nouvelle entité, sans identifiant : attribué par PostgreSQL à l'insertion.
     */
    public static ProjectEntity toNewEntity(Project project) {
        return ProjectEntity.builder()
            .title(project.title())
            .slug(project.slug())
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
            .build();
    }
}
