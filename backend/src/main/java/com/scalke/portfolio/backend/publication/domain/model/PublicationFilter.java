package com.scalke.portfolio.backend.publication.domain.model;

/**
 * Critères résolus transmis au port : type, et identifiants de catégorie et de tag (D-AQ).
 * Chaque critère {@code null} est ignoré ; les critères présents se cumulent (ET).
 * <p>
 * Les slugs reçus par l'API sont traduits en identifiants par le cas d'usage, via la façade du module
 * {@code taxonomy} : le port ne connaît pas les slugs de la taxonomie.
 */
public record PublicationFilter(PublicationType type, Long categoryId, Long tagId) {

    private static final PublicationFilter NONE = new PublicationFilter(null, null, null);

    public static PublicationFilter none() {
        return NONE;
    }

    public boolean hasType() {
        return type != null;
    }

    public boolean hasCategory() {
        return categoryId != null;
    }

    public boolean hasTag() {
        return tagId != null;
    }
}
