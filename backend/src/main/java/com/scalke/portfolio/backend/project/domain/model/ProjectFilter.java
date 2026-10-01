package com.scalke.portfolio.backend.project.domain.model;

/**
 * Critères de la liste publique des projets. {@code technologySlug == null} : toutes les technologies (D-AC) ;
 * {@code featured == null} : mis en avant ou non, {@code true} : mis en avant seulement, {@code false} : les autres
 * (F17, accueil). Les deux critères se combinent.
 * <p>
 * Un slug inconnu n'est pas une erreur : il produit une page vide, comme tout filtre sans résultat.
 */
public record ProjectFilter(String technologySlug, Boolean featured) {

    private static final ProjectFilter NONE = new ProjectFilter(null, null);

    public static ProjectFilter none() {
        return NONE;
    }

    /**
     * Filtre sur une technologie ; une valeur absente ou vide signifie « aucun filtre ».
     */
    public static ProjectFilter byTechnology(String technologySlug) {
        return of(technologySlug, null);
    }

    /**
     * Filtre de la requête publique : technologie (absente ou vide : aucune) et mise en avant (absente : aucune).
     */
    public static ProjectFilter of(String technologySlug, Boolean featured) {
        String technology = technologySlug == null || technologySlug.isBlank() ? null : technologySlug.strip();
        return technology == null && featured == null ? NONE : new ProjectFilter(technology, featured);
    }

    public boolean hasTechnology() {
        return technologySlug != null;
    }

    public boolean hasFeatured() {
        return featured != null;
    }
}
