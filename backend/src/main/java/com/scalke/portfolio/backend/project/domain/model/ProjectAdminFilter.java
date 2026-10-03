package com.scalke.portfolio.backend.project.domain.model;

/**
 * Critères de la liste d'administration des projets (F27). {@code visibility == null} : toutes les visibilités ;
 * {@code technologySlug == null} : toutes les technologies. Les deux critères se combinent ; un slug inconnu produit
 * une page vide, comme sur le site (D-AC).
 */
public record ProjectAdminFilter(ProjectVisibility visibility, String technologySlug) {

    /**
     * Filtre de la requête : visibilité (absente : aucune) et technologie (absente ou vide : aucune).
     */
    public static ProjectAdminFilter of(ProjectVisibility visibility, String technologySlug) {
        String technology = technologySlug == null || technologySlug.isBlank() ? null : technologySlug.strip();
        return new ProjectAdminFilter(visibility, technology);
    }

    public boolean hasVisibility() {
        return visibility != null;
    }

    public boolean hasTechnology() {
        return technologySlug != null;
    }
}
