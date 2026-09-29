package com.scalke.portfolio.backend.series.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.series.domain.model.SeriesItem;
import com.scalke.portfolio.backend.series.infrastructure.persistence.jpa.entity.SeriesEntity;
import com.scalke.portfolio.backend.series.infrastructure.persistence.jpa.entity.SeriesItemEmbeddable;
import com.scalke.portfolio.backend.shared.domain.model.Slug;

import java.util.List;

public final class SeriesPersistenceMapper {

    private SeriesPersistenceMapper() {
    }

    public static Series toDomain(SeriesEntity entity) {
        return new Series(
            entity.getId(),
            entity.getTitle(),
            Slug.of(entity.getSlug()),
            entity.getDescriptionMarkdown(),
            entity.getItems().stream()
                .map(item -> new SeriesItem(item.getPublicationId(), item.getPosition()))
                .toList(),
            entity.getCoverMediaId());
    }

    /**
     * Nouvelle entité, sans identifiant : attribué par PostgreSQL à l'insertion.
     */
    public static SeriesEntity toNewEntity(Series series) {
        return SeriesEntity.builder()
            .title(series.title())
            .slug(series.slug().value())
            .descriptionMarkdown(series.descriptionMarkdown())
            .items(items(series))
            .coverMediaId(series.coverMediaId())
            .build();
    }

    public static List<SeriesItemEmbeddable> items(Series series) {
        return series.items().stream()
            .map(item -> new SeriesItemEmbeddable(item.publicationId(), item.position()))
            .toList();
    }
}
