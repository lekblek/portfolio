package com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;
import com.scalke.portfolio.backend.taxonomy.infrastructure.persistence.jpa.entity.TagEntity;

public final class TagPersistenceMapper {

    private TagPersistenceMapper() {
    }

    public static Tag toDomain(TagEntity entity) {
        return new Tag(entity.getId(), entity.getName(), entity.getSlug());
    }

    /**
     * Nouvelle entité, sans identifiant : attribué par PostgreSQL à l'insertion.
     */
    public static TagEntity toNewEntity(Tag tag) {
        return TagEntity.builder()
            .name(tag.name())
            .slug(tag.slug())
            .build();
    }
}
