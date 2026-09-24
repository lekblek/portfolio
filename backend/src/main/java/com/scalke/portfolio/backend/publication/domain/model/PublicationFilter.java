package com.scalke.portfolio.backend.publication.domain.model;

/**
 * Critères de la liste publique des publications. {@code type == null} : articles et news.
 * Catégorie et tag s'ajouteront à l'étape 20.
 */
public record PublicationFilter(PublicationType type) {

    private static final PublicationFilter NONE = new PublicationFilter(null);

    public static PublicationFilter none() {
        return NONE;
    }

    public static PublicationFilter ofType(PublicationType type) {
        return type == null ? NONE : new PublicationFilter(type);
    }

    public boolean hasType() {
        return type != null;
    }
}
