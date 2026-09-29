package com.scalke.portfolio.backend.shared.domain.model;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Identifiant public lisible d'une ressource (projet, publication, technologie, catégorie, tag) :
 * minuscules ASCII, chiffres et tirets simples ({@code ^[a-z0-9]+(-[a-z0-9]+)*$}), au plus
 * {@value #MAX_LENGTH} caractères (D-BA).
 * <p>
 * Même format que les contraintes {@code *_slug_format_check} de PostgreSQL, qui restent la garantie
 * de dernier recours ; les longueurs propres à chaque table (160, 80, 60) sont garanties par la base.
 */
public record Slug(String value) {

    public static final int MAX_LENGTH = 160;

    /**
     * Longueur maximale d'un slug généré : une URL lisible, coupée entre deux mots.
     */
    static final int MAX_GENERATED_LENGTH = 80;

    /**
     * Nombre maximal de suffixes essayés pour résoudre une collision ({@code -2} … {@code -99}).
     */
    static final int MAX_SUFFIX = 99;

    private static final Pattern FORMAT = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    /**
     * Lettres que la décomposition Unicode ne ramène pas à l'ASCII.
     */
    private static final Map<String, String> LIGATURES = Map.of(
        "œ", "oe", "æ", "ae", "ß", "ss", "ø", "o", "đ", "d", "ł", "l");

    public Slug {
        Objects.requireNonNull(value, "value");
        if (!isValid(value)) {
            throw new IllegalArgumentException("invalid slug: \"" + value + "\"");
        }
    }

    public static Slug of(String value) {
        return new Slug(value);
    }

    /**
     * Pour une valeur non fiable (chemin d'URL) : vide si elle ne peut pas être un slug, ce qui permet de
     * répondre « introuvable » sans interroger la base.
     */
    public static Optional<Slug> parse(String candidate) {
        return candidate != null && isValid(candidate) ? Optional.of(new Slug(candidate)) : Optional.empty();
    }

    /**
     * Génère un slug depuis un titre ou un nom : accents retirés, ligatures translittérées, tout autre
     * caractère remplacé par un tiret, au plus {@value #MAX_GENERATED_LENGTH} caractères coupés entre deux
     * mots. « Construire une API REST avec Spring Boot » → {@code construire-une-api-rest-avec-spring-boot}.
     *
     * @throws IllegalArgumentException si le texte ne contient ni lettre ni chiffre
     */
    public static Slug fromText(String text) {
        return fromText(text, MAX_GENERATED_LENGTH);
    }

    /**
     * Comme {@link #fromText(String)}, au plus {@code maxLength} caractères : pour un vocabulaire dont la colonne est
     * plus courte (catégories 80, tags 60, D-CS).
     */
    public static Slug fromText(String text, int maxLength) {
        String lower = Objects.requireNonNull(text, "text").toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> ligature : LIGATURES.entrySet()) {
            lower = lower.replace(ligature.getKey(), ligature.getValue());
        }
        String ascii = COMBINING_MARKS.matcher(Normalizer.normalize(lower, Normalizer.Form.NFKD)).replaceAll("");
        String hyphenated = trimHyphens(NON_ALPHANUMERIC.matcher(ascii).replaceAll("-"));
        if (hyphenated.isEmpty()) {
            throw new IllegalArgumentException("no letter or digit to build a slug from: \"" + text + "\"");
        }
        return new Slug(truncateBetweenWords(hyphenated, Math.min(maxLength, MAX_GENERATED_LENGTH)));
    }

    /**
     * Ce slug s'il est libre, sinon le premier libre parmi {@code slug-2} … {@code slug-99} (D-BA).
     * L'unicité reste garantie par la contrainte {@code UNIQUE} de la base en cas d'écritures concurrentes.
     *
     * @throws IllegalStateException si aucun suffixe n'est libre
     */
    public Slug firstAvailable(Predicate<Slug> isTaken) {
        return firstAvailable(isTaken, MAX_LENGTH);
    }

    /**
     * Comme {@link #firstAvailable(Predicate)}, le suffixe ne faisant jamais dépasser {@code maxLength} (D-CS).
     */
    public Slug firstAvailable(Predicate<Slug> isTaken, int maxLength) {
        if (!isTaken.test(this)) {
            return this;
        }
        for (int suffix = 2; suffix <= MAX_SUFFIX; suffix++) {
            Slug candidate = withSuffix(suffix, maxLength);
            if (!isTaken.test(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("no free slug from " + value + "-2 to " + value + "-" + MAX_SUFFIX);
    }

    Slug withSuffix(int suffix) {
        return withSuffix(suffix, MAX_LENGTH);
    }

    Slug withSuffix(int suffix, int maxLength) {
        String end = "-" + suffix;
        String base = value.length() + end.length() > maxLength
            ? trimHyphens(value.substring(0, maxLength - end.length()))
            : value;
        return new Slug(base + end);
    }

    @Override
    public String toString() {
        return value;
    }

    private static boolean isValid(String value) {
        return value.length() <= MAX_LENGTH && FORMAT.matcher(value).matches();
    }

    private static String truncateBetweenWords(String slug, int maxLength) {
        if (slug.length() <= maxLength) {
            return slug;
        }
        String cut = slug.substring(0, maxLength);
        int lastHyphen = cut.lastIndexOf('-');
        boolean cutInsideAWord = slug.charAt(maxLength) != '-';
        return trimHyphens(cutInsideAWord && lastHyphen > 0 ? cut.substring(0, lastHyphen) : cut);
    }

    private static String trimHyphens(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '-') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == '-') {
            end--;
        }
        return value.substring(start, end);
    }
}
