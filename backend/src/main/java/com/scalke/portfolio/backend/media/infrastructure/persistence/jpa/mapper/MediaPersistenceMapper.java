package com.scalke.portfolio.backend.media.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.media.domain.model.Dimensions;
import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.domain.model.StorageKey;
import com.scalke.portfolio.backend.media.infrastructure.persistence.jpa.entity.MediaEntity;

public final class MediaPersistenceMapper {

    private MediaPersistenceMapper() {
    }

    public static Media toDomain(MediaEntity entity) {
        return new Media(
            entity.getId(),
            new StorageKey(entity.getStorageKey()),
            entity.getOriginalName(),
            entity.getSizeBytes(),
            entity.getWidth() == null ? null : new Dimensions(entity.getWidth(), entity.getHeight()),
            entity.getAltText(),
            entity.getCreatedAt());
    }

    /**
     * Nouvelle entité, sans identifiant : attribué par PostgreSQL à l'insertion.
     */
    public static MediaEntity toNewEntity(Media media) {
        return MediaEntity.builder()
            .storageKey(media.storageKey().value())
            .originalName(media.originalName())
            .sizeBytes(media.size())
            .width(media.dimensions() == null ? null : media.dimensions().width())
            .height(media.dimensions() == null ? null : media.dimensions().height())
            .altText(media.altText())
            .createdAt(media.createdAt())
            .build();
    }
}
