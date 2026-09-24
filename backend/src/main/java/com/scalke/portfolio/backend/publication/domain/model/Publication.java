package com.scalke.portfolio.backend.publication.domain.model;

import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Publication (racine d'agrégat), modèle commun à {@code ARTICLE} et {@code NEWS}. Le Markdown est la
 * source canonique : aucun HTML n'est stocké.
 * <p>
 * Invariant 24 (D-AI, D-AV) : une publication {@code SCHEDULED}, {@code PUBLISHED} ou {@code ARCHIVED} a une
 * date de publication. Doublé par {@code publication_published_at_check}. Invariant 26 : le statut ne change
 * que par {@link #transitionTo} (table dans {@link PublicationStatus}). Le slug est unique pour toutes les publications et
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

    /**
     * Statut observable à {@code now} (D03) : une publication planifiée dont la date est passée est publiée,
     * même si son statut stocké reste {@code SCHEDULED}.
     */
    public PublicationStatus effectiveStatus(Instant now) {
        return status == PublicationStatus.SCHEDULED && !publishedAt.isAfter(now) ? PublicationStatus.PUBLISHED : status;
    }

    /**
     * Même règle que la requête publique ({@code PublicationSpecifications.visibleAt}) ; un test
     * d'intégration vérifie que les deux concordent (D-AY).
     */
    public boolean isVisibleAt(Instant now) {
        return effectiveStatus(now) == PublicationStatus.PUBLISHED;
    }

    /**
     * Change de statut éditorial (D-AV) et renvoie la publication modifiée ({@code updatedAt = now}).
     * <p>
     * Règles de date :
     * <ul>
     *   <li>{@code SCHEDULED} : {@code scheduledAt} obligatoire et strictement future ; elle devient la date de publication ;</li>
     *   <li>{@code PUBLISHED} : date conservée si la publication a déjà été publique (date passée), sinon {@code now} ;</li>
     *   <li>{@code DRAFT} : une planification encore future est annulée (date effacée) ; une date passée est conservée ;</li>
     *   <li>{@code IN_REVIEW}, {@code ARCHIVED} : date inchangée.</li>
     * </ul>
     * Toute transition refusée lève {@link BusinessRuleViolationException} avec
     * {@link ErrorCode#INVALID_PUBLICATION_TRANSITION} (409, D-AW).
     *
     * @param scheduledAt date de publication planifiée ; uniquement pour {@code SCHEDULED}, {@code null} sinon
     */
    public Publication transitionTo(PublicationStatus target, Instant scheduledAt, Instant now) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(now, "now");
        PublicationStatus current = effectiveStatus(now);
        if (scheduledAt != null && target != PublicationStatus.SCHEDULED) {
            throw refused("Une date de publication ne s'indique que pour une planification.");
        }
        if (!current.canMoveTo(target)) {
            throw refused("Transition interdite : " + current + " → " + target + ".");
        }
        Instant newPublishedAt = switch (target) {
            case SCHEDULED -> {
                if (scheduledAt == null || !scheduledAt.isAfter(now)) {
                    throw refused("Une planification exige une date de publication future.");
                }
                yield scheduledAt;
            }
            case PUBLISHED -> publishedAt != null && !publishedAt.isAfter(now) ? publishedAt : now;
            case DRAFT -> publishedAt != null && publishedAt.isAfter(now) ? null : publishedAt;
            case IN_REVIEW, ARCHIVED -> publishedAt;
        };
        return new Publication(id, type, title, slug, summary, contentMarkdown, target, newPublishedAt, featured,
            categoryId, tagIds, seoTitle, seoDescription, createdAt, now);
    }

    private static BusinessRuleViolationException refused(String detail) {
        return new BusinessRuleViolationException(ErrorCode.INVALID_PUBLICATION_TRANSITION, detail);
    }
}
