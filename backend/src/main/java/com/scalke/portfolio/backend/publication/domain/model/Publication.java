package com.scalke.portfolio.backend.publication.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Publication (racine d'agrégat), modèle commun à {@code ARTICLE} et {@code NEWS}. Le Markdown est la
 * source canonique : aucun HTML n'est stocké.
 * <p>
 * Invariant 24 (D-AI) : une publication {@code SCHEDULED} ou {@code PUBLISHED} a une date de publication.
 * Doublé par {@code publication_published_at_check}. Le slug est unique pour toutes les publications et
 * au format kebab-case, garanti par PostgreSQL (D-AL).
 * <p>
 * Classement (D-AN) : au plus une catégorie ({@code categoryId}, D20) et des tags ({@code tagIds}, un
 * ensemble : chaque tag au plus une fois). Les termes appartiennent au module {@code taxonomy} : la
 * publication n'en connaît que les identifiants. Couverture : étape 27.
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
    Long categoryId,
    Set<Long> tagIds,
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
        tagIds = Set.copyOf(Objects.requireNonNull(tagIds, "tagIds"));
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
