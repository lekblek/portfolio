package com.scalke.portfolio.backend.publication.domain.model;

/**
 * Statut éditorial (modèle {@code 02} §15). Visibilité publique (invariants 7 à 10, D-AH) :
 * {@code PUBLISHED}, ou {@code SCHEDULED} dont la date de publication est passée selon l'horloge
 * applicative ({@link Publication#effectiveStatus}).
 * <p>
 * Transitions autorisées (invariant 26, D-AV), évaluées sur le statut <em>effectif</em> :
 * <pre>
 * DRAFT      → IN_REVIEW, SCHEDULED, PUBLISHED
 * IN_REVIEW  → DRAFT, SCHEDULED, PUBLISHED
 * SCHEDULED  → DRAFT, SCHEDULED (nouvelle date), PUBLISHED      (date encore future)
 * PUBLISHED  → ARCHIVED                                         (y compris SCHEDULED dont la date est passée)
 * ARCHIVED   → DRAFT, PUBLISHED
 * </pre>
 */
public enum PublicationStatus {
    DRAFT,
    IN_REVIEW,
    SCHEDULED,
    PUBLISHED,
    ARCHIVED;

    /**
     * Statuts qui exigent une date de publication (invariant 24) : une publication planifiée, publiée ou
     * archivée (donc publiée auparavant) a toujours une date.
     */
    public boolean requiresPublicationDate() {
        return this == SCHEDULED || this == PUBLISHED || this == ARCHIVED;
    }

    /**
     * Table des transitions (invariant 26). {@code this} est le statut effectif de la publication.
     */
    public boolean canMoveTo(PublicationStatus target) {
        return switch (this) {
            case DRAFT -> target == IN_REVIEW || target == SCHEDULED || target == PUBLISHED;
            case IN_REVIEW -> target == DRAFT || target == SCHEDULED || target == PUBLISHED;
            case SCHEDULED -> target == DRAFT || target == SCHEDULED || target == PUBLISHED;
            case PUBLISHED -> target == ARCHIVED;
            case ARCHIVED -> target == DRAFT || target == PUBLISHED;
        };
    }
}
