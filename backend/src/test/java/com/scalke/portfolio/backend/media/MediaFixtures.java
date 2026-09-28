package com.scalke.portfolio.backend.media;

import com.scalke.portfolio.backend.media.domain.model.Dimensions;
import com.scalke.portfolio.backend.media.domain.model.Media;
import com.scalke.portfolio.backend.media.domain.model.MediaFormat;
import com.scalke.portfolio.backend.media.domain.model.StorageKey;
import com.scalke.portfolio.backend.testsupport.FixedClockConfiguration;

/**
 * Entrées du catalogue pour les tests, sans fichier stocké : à créer par {@code MediaRepository.create} quand
 * seul le catalogue compte (références, façade).
 */
public final class MediaFixtures {

    private MediaFixtures() {
    }

    public static Media image(String altText) {
        return new Media(null, StorageKey.random(MediaFormat.WEBP), "image.webp", 1_024, new Dimensions(1200, 630),
            altText, FixedClockConfiguration.NOW);
    }

    public static Media pdf() {
        return new Media(null, StorageKey.random(MediaFormat.PDF), "cv.pdf", 2_048, null, null,
            FixedClockConfiguration.NOW);
    }
}
