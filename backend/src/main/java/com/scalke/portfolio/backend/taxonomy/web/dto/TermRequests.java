package com.scalke.portfolio.backend.taxonomy.web.dto;

/**
 * Règles de saisie communes aux termes (D-CS).
 */
final class TermRequests {

    /**
     * Au moins une lettre ou un chiffre : sinon aucun slug ne pourrait être généré depuis le nom.
     */
    static final String HAS_LETTER_OR_DIGIT = "(?s).*[\\p{L}\\p{N}].*";
    static final String HAS_LETTER_OR_DIGIT_MESSAGE = "doit contenir au moins une lettre ou un chiffre";

    /**
     * Format des slugs (D-BA) : minuscules ASCII, chiffres et tirets simples.
     */
    static final String SLUG = "[a-z0-9]+(-[a-z0-9]+)*";
    static final String SLUG_MESSAGE = "minuscules sans accent, chiffres et tirets simples";

    private TermRequests() {
    }
}
