package com.scalke.portfolio.backend.project.domain.model;

/**
 * Critères de la liste publique des projets (D-AC). {@code technologySlug == null} : aucun filtre.
 * <p>
 * Un slug inconnu n'est pas une erreur : il produit une page vide, comme tout filtre sans résultat.
 */
public record ProjectFilter(String technologySlug) {

    private static final ProjectFilter NONE = new ProjectFilter(null);

    public static ProjectFilter none() {
        return NONE;
    }

    /**
     * Filtre sur une technologie ; une valeur absente ou vide signifie « aucun filtre ».
     */
    public static ProjectFilter byTechnology(String technologySlug) {
        return technologySlug == null || technologySlug.isBlank() ? NONE : new ProjectFilter(technologySlug.strip());
    }

    public boolean hasTechnology() {
        return technologySlug != null;
    }
}
