package com.scalke.portfolio.backend.publication.application.usecase;

import com.scalke.portfolio.backend.publication.domain.model.PublicationType;

/**
 * Critères de la liste publique tels que le client les exprime : type, slug de catégorie, slug de tag.
 * Une valeur absente ou vide signifie « pas de filtre » ; les critères présents se cumulent (D-AQ).
 */
public record PublicationCriteria(PublicationType type, String categorySlug, String tagSlug) {

    public static PublicationCriteria of(PublicationType type, String categorySlug, String tagSlug) {
        return new PublicationCriteria(type, normalize(categorySlug), normalize(tagSlug));
    }

    public static PublicationCriteria none() {
        return new PublicationCriteria(null, null, null);
    }

    private static String normalize(String slug) {
        return slug == null || slug.isBlank() ? null : slug.strip();
    }
}
