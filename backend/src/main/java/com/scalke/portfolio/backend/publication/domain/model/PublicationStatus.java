package com.scalke.portfolio.backend.publication.domain.model;

/**
 * Statut éditorial (modèle {@code 02} §15). Visibilité publique (invariants 7 à 10, D-AH) :
 * {@code PUBLISHED}, ou {@code SCHEDULED} dont la date de publication est passée selon l'horloge
 * applicative. Les transitions autorisées arrivent à l'étape 21.
 */
public enum PublicationStatus {
    DRAFT,
    IN_REVIEW,
    SCHEDULED,
    PUBLISHED,
    ARCHIVED;

    /**
     * Statuts qui exigent une date de publication (invariant 24).
     */
    public boolean requiresPublicationDate() {
        return this == SCHEDULED || this == PUBLISHED;
    }
}
