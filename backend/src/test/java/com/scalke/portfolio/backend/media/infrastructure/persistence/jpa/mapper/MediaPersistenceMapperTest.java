package com.scalke.portfolio.backend.media.infrastructure.persistence.jpa.mapper;

import com.scalke.portfolio.backend.media.domain.model.Dimensions;
import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.domain.model.StorageKey;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MediaPersistenceMapperTest {

    @ParameterizedTest
    @ValueSource(strings = {"png", "pdf"})
    void keeps_every_field_through_a_round_trip(String extension) {
        boolean image = extension.equals("png");
        Media media = new Media(null, new StorageKey("3f2a9c0e8d7b4a1f9e6c5b4a3d2e1f0a." + extension),
            "fichier." + extension, 2_048, image ? new Dimensions(1200, 630) : null, image ? "Texte" : null,
            Instant.parse("2026-06-15T10:00:00Z"));

        assertThat(MediaPersistenceMapper.toDomain(MediaPersistenceMapper.toNewEntity(media))).isEqualTo(media);
    }
}
