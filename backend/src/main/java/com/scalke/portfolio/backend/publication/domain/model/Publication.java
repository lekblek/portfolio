package com.scalke.portfolio.backend.publication.domain.model;

import com.scalke.portfolio.backend.shared.domain.model.Slug;
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
 * Dates :
 * <ul>
 *   <li>{@code publishedAt} : date affichée et date de planification ; une replanification la remplace ;</li>
 *   <li>{@code firstPublishedAt} (D-AZ) : première apparition publique. Provisoire tant qu'elle est future
 *       (planification), elle ne change plus dès qu'elle est passée, quelles que soient les transitions
 *       suivantes. C'est elle qui dit si la publication a déjà été publique ({@link #hasBeenPublic}).</li>
 * </ul>
 * Invariants : 24 (D-AI, D-AV, D-AZ) — {@code SCHEDULED}, {@code PUBLISHED} ou {@code ARCHIVED} ont
 * {@code publishedAt} et {@code firstPublishedAt}, et {@code firstPublishedAt <= publishedAt} ; 26 — le statut
 * ne change que par {@link #transitionTo} ; 6 (D11, D-BC) — le slug ne change plus après la première
 * publication ({@link #changeSlug}). 24 est doublé par PostgreSQL ; 6 et 26 sont des règles applicatives.
 * <p>
 * Classement (D-AN) : au plus une catégorie ({@code categoryId}, D20) et des tags ({@code tagIds}, un
 * ensemble : chaque tag au plus une fois). Les termes appartiennent au module {@code taxonomy} : la
 * publication n'en connaît que les identifiants. Couverture ({@code coverMediaId}, facultative) : référence
 * vers une image du catalogue {@code media}, par identifiant (ADR 0002, D-BY).
 * <p>
 * Bornes (D-CU) : titre non vide de 160 caractères au plus, résumé de 500, contenu de {@value #CONTENT_MAX_LENGTH}
 * (KI-31 : le document de recherche reste loin de la limite d'un {@code tsvector}), titre SEO de 120, description
 * SEO de 300. Les longueurs sont doublées par PostgreSQL (colonnes de {@code V006}, contrainte de {@code V022}).
 */
public record Publication(
    Long id,
    PublicationType type,
    String title,
    Slug slug,
    String summary,
    String contentMarkdown,
    PublicationStatus status,
    Instant publishedAt,
    Instant firstPublishedAt,
    boolean featured,
    Long categoryId,
    Set<Long> tagIds,
    String seoTitle,
    String seoDescription,
    Instant createdAt,
    Instant updatedAt,
    Long coverMediaId
) {

    /**
     * Vitesse de lecture retenue pour le temps de lecture (D12).
     */
    static final int WORDS_PER_MINUTE = 200;

    public static final int TITLE_MAX_LENGTH = 160;
    public static final int SUMMARY_MAX_LENGTH = 500;
    public static final int CONTENT_MAX_LENGTH = 100_000;
    public static final int SEO_TITLE_MAX_LENGTH = 120;
    public static final int SEO_DESCRIPTION_MAX_LENGTH = 300;

    /**
     * Un mot : lettres ou chiffres, éventuellement liés par une apostrophe ou un tiret (« l'API »,
     * « full-stack »). Les symboles Markdown ({@code #}, {@code *}, {@code -}) ne comptent pas.
     */
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+(?:['’-][\\p{L}\\p{N}]+)*");

    public Publication {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(slug, "slug");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(contentMarkdown, "contentMarkdown");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        tagIds = Set.copyOf(Objects.requireNonNull(tagIds, "tagIds"));
        if (title.isBlank() || title.length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("title must be 1 to 160 characters");
        }
        if (summary.length() > SUMMARY_MAX_LENGTH) {
            throw new IllegalArgumentException("summary must be at most 500 characters");
        }
        if (contentMarkdown.length() > CONTENT_MAX_LENGTH) {
            throw new IllegalArgumentException("content must be at most " + CONTENT_MAX_LENGTH + " characters");
        }
        if (seoTitle != null && seoTitle.length() > SEO_TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("SEO title must be at most 120 characters");
        }
        if (seoDescription != null && seoDescription.length() > SEO_DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("SEO description must be at most 300 characters");
        }
        if (status.requiresPublicationDate() && (publishedAt == null || firstPublishedAt == null)) {
            throw new IllegalArgumentException("a " + status + " publication requires publishedAt and firstPublishedAt");
        }
        if (firstPublishedAt != null && publishedAt != null && firstPublishedAt.isAfter(publishedAt)) {
            throw new IllegalArgumentException("firstPublishedAt must not be after publishedAt");
        }
    }

    /**
     * Nouveau brouillon (D-CU) : ni date de publication ni première publication ; créé et modifié à {@code now}.
     */
    public static Publication newDraft(PublicationType type, Slug slug, PublicationContent content, Instant now) {
        Objects.requireNonNull(content, "content");
        return new Publication(null, type, content.title(), slug, content.summary(), content.contentMarkdown(),
            PublicationStatus.DRAFT, null, null, content.featured(), content.categoryId(), content.tagIds(),
            content.seoTitle(), content.seoDescription(), now, now, content.coverMediaId());
    }

    /**
     * Remplace la saisie de l'administrateur (D-CU) ; le type, le statut et les dates du cycle éditorial ne changent
     * pas. Un nouveau slug suit la règle de {@link #changeSlug} ({@code SLUG_LOCKED} après la première publication).
     */
    public Publication edit(Slug newSlug, PublicationContent content, Instant now) {
        Objects.requireNonNull(content, "content");
        Publication renamed = changeSlug(newSlug, now);
        return new Publication(id, type, content.title(), renamed.slug(), content.summary(), content.contentMarkdown(),
            status, publishedAt, firstPublishedAt, content.featured(), content.categoryId(), content.tagIds(),
            content.seoTitle(), content.seoDescription(), createdAt, now, content.coverMediaId());
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
     * Vrai si la publication a été publique au moins une fois avant {@code now}, même si elle ne l'est plus
     * (archivée, repassée en brouillon, replanifiée) : sa première date de publication est passée (D-AZ).
     */
    public boolean hasBeenPublic(Instant now) {
        return firstPublishedAt != null && !firstPublishedAt.isAfter(now);
    }

    /**
     * Change de statut éditorial (D-AV) et renvoie la publication modifiée ({@code updatedAt = now}).
     * <p>
     * Règles de date :
     * <ul>
     *   <li>{@code SCHEDULED} : {@code scheduledAt} obligatoire et strictement future ; elle devient la date de publication ;</li>
     *   <li>{@code PUBLISHED} : date conservée si elle est déjà passée (restauration d'une archive), sinon {@code now} ;</li>
     *   <li>{@code DRAFT} : une planification encore future est annulée (date effacée) ; une date passée est conservée ;</li>
     *   <li>{@code IN_REVIEW}, {@code ARCHIVED} : date inchangée.</li>
     * </ul>
     * {@code firstPublishedAt} suit la date de publication tant que la publication n'a jamais été publique ;
     * dès qu'elle l'a été, elle ne change plus (D-AZ).
     * <p>
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
        boolean alreadyPublic = hasBeenPublic(now);
        Instant newFirstPublishedAt = switch (target) {
            case SCHEDULED, PUBLISHED -> alreadyPublic ? firstPublishedAt : newPublishedAt;
            case DRAFT -> alreadyPublic ? firstPublishedAt : null;
            case IN_REVIEW, ARCHIVED -> firstPublishedAt;
        };
        return new Publication(id, type, title, slug, summary, contentMarkdown, target, newPublishedAt,
            newFirstPublishedAt, featured, categoryId, tagIds, seoTitle, seoDescription, createdAt, now, coverMediaId);
    }

    /**
     * Change le slug (D11, D-BC) : permis tant que la publication n'a jamais été publique ; refusé ensuite
     * avec {@link ErrorCode#SLUG_LOCKED} (409), pour que les liens publiés restent valides. Sans effet si le
     * slug est identique. L'unicité est vérifiée par l'appelant ({@link Slug#firstAvailable}) et par la base.
     * <p>
     * Appelé par la modification d'une publication ({@link #edit}, D-CU).
     */
    public Publication changeSlug(Slug newSlug, Instant now) {
        Objects.requireNonNull(newSlug, "newSlug");
        Objects.requireNonNull(now, "now");
        if (newSlug.equals(slug)) {
            return this;
        }
        if (hasBeenPublic(now)) {
            throw new BusinessRuleViolationException(ErrorCode.SLUG_LOCKED,
                "Le slug d'une publication déjà publiée ne peut plus changer.");
        }
        return new Publication(id, type, title, newSlug, summary, contentMarkdown, status, publishedAt,
            firstPublishedAt, featured, categoryId, tagIds, seoTitle, seoDescription, createdAt, now, coverMediaId);
    }

    private static BusinessRuleViolationException refused(String detail) {
        return new BusinessRuleViolationException(ErrorCode.INVALID_PUBLICATION_TRANSITION, detail);
    }
}
