package com.scalke.portfolio.backend.project.domain.model;

/**
 * Visibilité publique d'un projet. Seul {@code PUBLISHED} est exposé par l'API publique
 * (invariant 11, D-U) ; les autres valeurs y sont indiscernables d'un projet inexistant (404).
 */
public enum ProjectVisibility {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
