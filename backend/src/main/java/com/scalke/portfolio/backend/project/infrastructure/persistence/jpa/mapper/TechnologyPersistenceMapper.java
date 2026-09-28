package com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.project.infrastructure.persistence.jpa.entity.TechnologyEntity;
import com.scalke.portfolio.backend.shared.domain.model.Slug;

import java.util.List;

public final class TechnologyPersistenceMapper {

    private TechnologyPersistenceMapper() {
    }

    public static Technology toDomain(TechnologyEntity entity) {
        return new Technology(entity.getId(), entity.getName(), Slug.of(entity.getSlug()), entity.getDisplayOrder());
    }

    public static List<Technology> map(List<TechnologyEntity> entities) {
        return entities.stream().map(TechnologyPersistenceMapper::toDomain).toList();
    }

    /**
     * Nouvelle entité, sans identifiant : attribué par PostgreSQL à l'insertion.
     */
    public static TechnologyEntity toNewEntity(Technology technology) {
        return TechnologyEntity.builder()
            .name(technology.name())
            .slug(technology.slug().value())
            .displayOrder(technology.displayOrder())
            .build();
    }
}
