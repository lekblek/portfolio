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

    /**
     * Adresse web absolue (D-CX) : {@code http://} ou {@code https://}, sans espace. Exclut {@code javascript:} et les
     * autres schémas qu'un lien publié ne doit pas porter.
     */
    public static final String HTTP_URL = "https?://\\S+";
    public static final String HTTP_URL_MESSAGE = "adresse http:// ou https:// attendue";

    private InputPatterns() {
    }
}
