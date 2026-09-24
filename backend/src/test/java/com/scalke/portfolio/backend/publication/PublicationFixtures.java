package com.scalke.portfolio.backend.publication;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.testsupport.FixedClockConfiguration;

import java.time.Instant;

/**
 * Publications du domaine pour les tests. Les dates d'audit valent l'instant fixe des tests d'intégration.
 */
public final class PublicationFixtures {

    private PublicationFixtures() {
    }

    public static Publication publication(String slug, PublicationType type, PublicationStatus status,
                                          Instant publishedAt) {
        Instant now = FixedClockConfiguration.NOW;
        return new Publication(
            null,
            type,
            "Titre " + slug,
            slug,
            "Résumé de " + slug,
            "# " + slug + "\n\nContenu.",
            status,
            publishedAt,
            false,
            null,
            null,
            now,
            now);
    }

    public static Publication article(String slug, PublicationStatus status, Instant publishedAt) {
        return publication(slug, PublicationType.ARTICLE, status, publishedAt);
    }
}
