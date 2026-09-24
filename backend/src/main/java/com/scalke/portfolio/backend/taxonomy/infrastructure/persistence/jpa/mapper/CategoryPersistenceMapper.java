package com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.taxonomy.domain.model.Category;
import com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.entity.CategoryEntity;

public final class CategoryPersistenceMapper {

    private CategoryPersistenceMapper() {
    }

    public static Category toDomain(CategoryEntity entity) {
        return new Category(entity.getId(), entity.getName(), entity.getSlug(), entity.getDescription());
    }

    /**
     * Nouvelle entité, sans identifiant : attribué par PostgreSQL à l'insertion.
     */
    public static CategoryEntity toNewEntity(Category category) {
        return CategoryEntity.builder()
            .name(category.name())
            .slug(category.slug())
            .description(category.description())
            .build();
    }
}
