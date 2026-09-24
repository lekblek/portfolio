package com.scalke.portfolio.backend.publication;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationStatus;
import com.scalke.portfolio.backend.publication.domain.model.PublicationType;
import com.scalke.portfolio.backend.testsupport.FixedClockConfiguration;

import java.time.Instant;
import java.util.Set;

/**
 * Publications du domaine pour les tests. Les dates d'audit valent l'instant fixe des tests d'intégration.
 */
public final class PublicationFixtures {

    private PublicationFixtures() {
    }

    public static Publication publication(String slug, PublicationType type, PublicationStatus status,
                                          Instant publishedAt) {
        return publication(slug, type, status, publishedAt, null, Set.of());
    }

    public static Publication article(String slug, PublicationStatus status, Instant publishedAt) {
        return publication(slug, PublicationType.ARTICLE, status, publishedAt);
    }

    /**
     * Article publié à {@code publishedAt}, classé dans la catégorie et les tags donnés (identifiants
     * de termes déjà créés dans le module taxonomy).
     */
    public static Publication classifiedArticle(String slug, Instant publishedAt, Long categoryId, Long... tagIds) {
        return publication(slug, PublicationType.ARTICLE, PublicationStatus.PUBLISHED, publishedAt,
            categoryId, Set.of(tagIds));
    }

    private static Publication publication(String slug, PublicationType type, PublicationStatus status,
                                           Instant publishedAt, Long categoryId, Set<Long> tagIds) {
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
            categoryId,
            tagIds,
            null,
            null,
            now,
            now);
    }
}
