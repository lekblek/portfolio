package com.scalke.portfolio.backend.contact.domain.model;

/**
 * Statut de traitement d'un message de contact ({@code 02} §24), dans l'ordre du cycle.
 * <p>
 * Invariant 29 (D-CH) : un message n'avance que dans cet ordre, en sautant éventuellement des étapes (un
 * message indésirable peut être archivé sans avoir été traité), et ne revient jamais en arrière.
 * <pre>
 * NEW       → READ, PROCESSED, ARCHIVED
 * READ      → PROCESSED, ARCHIVED
 * PROCESSED → ARCHIVED
 * ARCHIVED  → aucun
 * </pre>
 */
public enum ContactStatus {
    NEW,
    READ,
    PROCESSED,
    ARCHIVED;

    public boolean canMoveTo(ContactStatus target) {
        return target.ordinal() > ordinal();
    }
}
