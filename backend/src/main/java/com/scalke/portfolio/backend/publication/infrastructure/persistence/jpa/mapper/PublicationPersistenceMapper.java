package com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.infrastructure.persistence.jpa.entity.PublicationEntity;

import java.util.Set;

public final class PublicationPersistenceMapper {

    private PublicationPersistenceMapper() {
    }

    /**
     * Reconstruit la {@link Publication} du domaine : ses invariants sont revérifiés à chaque lecture.
     * Parcourt les tags : à appeler dans la transaction qui a chargé l'entité.
     */
    public static Publication toDomain(PublicationEntity entity) {
        return new Publication(
            entity.getId(),
            entity.getType(),
            entity.getTitle(),
            entity.getSlug(),
            entity.getSummary(),
            entity.getContentMarkdown(),
            entity.getStatus(),
            entity.getPublishedAt(),
            entity.isFeatured(),
            entity.getCategoryId(),
            Set.copyOf(entity.getTagIds()),
            entity.getSeoTitle(),
            entity.getSeoDescription(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    /**
     * Nouvelle entité, sans identifiant : attribué par PostgreSQL à l'insertion.
     */
    public static PublicationEntity toNewEntity(Publication publication) {
        return PublicationEntity.builder()
            .type(publication.type())
            .title(publication.title())
            .slug(publication.slug())
            .summary(publication.summary())
            .contentMarkdown(publication.contentMarkdown())
            .status(publication.status())
            .publishedAt(publication.publishedAt())
            .featured(publication.featured())
            .categoryId(publication.categoryId())
            .tagIds(publication.tagIds())
            .seoTitle(publication.seoTitle())
            .seoDescription(publication.seoDescription())
            .createdAt(publication.createdAt())
            .updatedAt(publication.updatedAt())
            .build();
    }
}
