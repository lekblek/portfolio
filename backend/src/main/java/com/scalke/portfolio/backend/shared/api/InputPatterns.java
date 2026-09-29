package com.scalke.portfolio.backend.shared.api;

/**
 * Motifs de saisie communs aux requêtes d'administration (D-CS, D-CU).
 */
public final class InputPatterns {

    /**
     * Au moins une lettre ou un chiffre : sinon aucun slug ne pourrait être généré depuis le nom.
     */
    public static final String HAS_LETTER_OR_DIGIT = "(?s).*[\\p{L}\\p{N}].*";
    public static final String HAS_LETTER_OR_DIGIT_MESSAGE = "doit contenir au moins une lettre ou un chiffre";

    /**
     * Format des slugs (D-BA) : minuscules ASCII, chiffres et tirets simples.
     */
    public static final String SLUG = "[a-z0-9]+(-[a-z0-9]+)*";
    public static final String SLUG_MESSAGE = "minuscules sans accent, chiffres et tirets simples";

    private InputPatterns() {
    }
}
