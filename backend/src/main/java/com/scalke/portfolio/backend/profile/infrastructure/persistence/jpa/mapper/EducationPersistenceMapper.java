package com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.profile.domain.model.Education;
import com.scalke.portfolio.backend.profile.infrastructure.persistence.jpa.entity.EducationEntity;
import com.scalke.portfolio.backend.shared.domain.model.DateRange;

import java.util.List;

public final class EducationPersistenceMapper {

    private EducationPersistenceMapper() {
    }

    public static Education toDomain(EducationEntity entity) {
        return new Education(
            entity.getId(),
            entity.getInstitution(),
            entity.getDegree(),
            entity.getField(),
            entity.getLocation(),
            new DateRange(entity.getStartDate(), entity.getEndDate()),
            entity.getDescription(),
            entity.getDisplayOrder()
        );
    }

    public static List<Education> map(List<EducationEntity> entities) {
        return entities.stream().map(EducationPersistenceMapper::toDomain).toList();
    }

    public static EducationEntity toEntity(Education education) {
        return EducationEntity.builder()
            .institution(education.institution())
            .degree(education.degree())
            .field(education.field())
            .location(education.location())
            .startDate(education.period().startDate())
            .endDate(education.period().endDate())
            .description(education.description())
            .displayOrder(education.displayOrder())
            .build();
    }
}
