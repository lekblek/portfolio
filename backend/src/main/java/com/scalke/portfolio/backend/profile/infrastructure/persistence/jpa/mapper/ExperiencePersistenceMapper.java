package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.profile.domain.model.Experience;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.ExperienceEntity;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;

import java.util.List;

public final class ExperiencePersistenceMapper {

    private ExperiencePersistenceMapper() {
    }

    public static Experience toDomain(ExperienceEntity entity) {
        return new Experience(
            entity.getId(),
            entity.getOrganization(),
            entity.getTitle(),
            entity.getLocation(),
            new DateRange(entity.getStartDate(), entity.getEndDate()),
            entity.getDescription(),
            entity.getDisplayOrder()
        );
    }

    public static List<Experience> map(List<ExperienceEntity> entities) {
        return entities.stream().map(ExperiencePersistenceMapper::toDomain).toList();
    }

    public static ExperienceEntity toEntity(Experience experience) {
        return ExperienceEntity.builder()
            .organization(experience.organization())
            .title(experience.title())
            .location(experience.location())
            .startDate(experience.period().startDate())
            .endDate(experience.period().endDate())
            .description(experience.description())
            .displayOrder(experience.displayOrder())
            .build();
    }
}
