package com.scalke.portfolio.backend.publication.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Publication (racine d'agrégat), modèle commun à {@code ARTICLE} et {@code NEWS}. Le Markdown est la
 * source canonique : aucun HTML n'est stocké.
 * <p>
 * Invariant 24 (D-AI) : une publication {@code SCHEDULED} ou {@code PUBLISHED} a une date de publication.
 * Doublé par {@code publication_published_at_check}. Le slug est unique pour toutes les publications et
 * au format kebab-case, garanti par PostgreSQL (D-AL).
 * <p>
 * Catégorie et tags (étape 20), couverture (étape 27) : pas encore dans le modèle (D-AF).
 */
public record Publication(
    Long id,
    PublicationType type,
    String title,
    String slug,
    String summary,
    String contentMarkdown,
    PublicationStatus status,
    Instant publishedAt,
    boolean featured,
    String seoTitle,
    String seoDescription,
    Instant createdAt,
    Instant updatedAt
) {

    /**
     * Vitesse de lecture retenue pour le temps de lecture (D12).
     */
    static final int WORDS_PER_MINUTE = 200;

    /**
     * Un mot : lettres ou chiffres, éventuellement liés par une apostrophe ou un tiret (« l'API »,
     * « full-stack »). Les symboles Markdown ({@code #}, {@code *}, {@code -}) ne comptent pas.
     */
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+(?:['’-][\\p{L}\\p{N}]+)*");

    public Publication {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(contentMarkdown, "contentMarkdown");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (status.requiresPublicationDate() && publishedAt == null) {
            throw new IllegalArgumentException("a " + status + " publication requires publishedAt");
        }
    }

    /**
     * Temps de lecture calculé (D12) : nombre de mots du Markdown, 200 mots par minute, au moins une minute.
     */
    public int readingTimeMinutes() {
        long words = WORD.matcher(contentMarkdown).results().count();
        return (int) Math.max(1, Math.ceilDiv(words, WORDS_PER_MINUTE));
    }
}
