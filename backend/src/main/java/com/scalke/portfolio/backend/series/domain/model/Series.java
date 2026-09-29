package com.scalke.portfolio.backend.series.domain.model;

import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException;
import com.scalke.portfolio.backend.shared.error.ErrorCode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Série d'articles (racine d'agrégat, D05) : liste ordonnée d'articles du module {@code publication},
 * référencés par identifiant (ADR 0002). Pas de workflow éditorial propre (01 §8) : une série est publique
 * si et seulement si au moins un de ses articles est visible (D-BG). Couverture ({@code coverMediaId},
 * facultative) : référence vers une image du catalogue {@code media}, par identifiant (D-BY).
 * <p>
 * Invariants : une position au plus une fois (invariant 3), positions strictement positives, un article au
 * plus une fois. Doublés par PostgreSQL, qui garantit aussi qu'un article n'appartient qu'à une série et
 * qu'une {@code NEWS} n'y entre jamais (invariants 1 et 2, D-BF). Le slug est un {@link Slug} (D-BA) ; il ne change
 * plus dès qu'un de ses articles a été public (D11, D-BK, {@link #edit}).
 * <p>
 * Bornes (D-CV) : titre non vide de 160 caractères au plus (colonne de {@code V011}), description de
 * {@value #DESCRIPTION_MAX_LENGTH} caractères au plus (vide permise).
 */
public record Series(
    Long id,
    String title,
    Slug slug,
    String descriptionMarkdown,
    List<SeriesItem> items,
    Long coverMediaId
) {

    public static final int TITLE_MAX_LENGTH = 160;
    public static final int DESCRIPTION_MAX_LENGTH = 10_000;

    public Series {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(slug, "slug");
        Objects.requireNonNull(descriptionMarkdown, "descriptionMarkdown");
        if (title.isBlank() || title.length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("title must be 1 to 160 characters");
        }
        if (descriptionMarkdown.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("description must be at most " + DESCRIPTION_MAX_LENGTH + " characters");
        }
        items = Objects.requireNonNull(items, "items").stream()
            .sorted(SeriesItem.BY_POSITION)
            .toList();
        Set<Integer> positions = new HashSet<>();
        Set<Long> publications = new HashSet<>();
        for (SeriesItem item : items) {
            if (!positions.add(item.position())) {
                throw new IllegalArgumentException("position " + item.position() + " appears twice");
            }
            if (!publications.add(item.publicationId())) {
                throw new IllegalArgumentException("publication " + item.publicationId() + " appears twice");
            }
        }
    }

    /**
     * Nouvelle série, sans chapitre (D-CV) : les articles sont rangés ensuite ({@link #withChapters}).
     */
    public static Series newSeries(Slug slug, SeriesContent content) {
        Objects.requireNonNull(content, "content");
        return new Series(null, content.title(), slug, content.descriptionMarkdown(), List.of(), content.coverMediaId());
    }

    /**
     * Remplace la saisie de l'administrateur (D-CV) ; les chapitres ne changent pas. Un slug différent est refusé
     * si la série a déjà été publique ({@code slugLocked} : un de ses articles l'a été, D-BK), avec
     * {@link ErrorCode#SLUG_LOCKED} (409).
     */
    public Series edit(Slug newSlug, SeriesContent content, boolean slugLocked) {
        Objects.requireNonNull(newSlug, "newSlug");
        Objects.requireNonNull(content, "content");
        if (slugLocked && !newSlug.equals(slug)) {
            throw new BusinessRuleViolationException(ErrorCode.SLUG_LOCKED,
                "Le slug d'une série déjà publiée ne peut plus changer.");
        }
        return new Series(id, content.title(), newSlug, content.descriptionMarkdown(), items, content.coverMediaId());
    }

    /**
     * Même série avec ces articles, dans cet ordre, aux positions 1, 2, … (D-CV) : une liste de chapitres est
     * remplacée d'un bloc, si bien qu'aucune position ne peut être occupée deux fois.
     */
    public Series withChapters(List<Long> publicationIds) {
        List<SeriesItem> chapters = new ArrayList<>();
        for (Long publicationId : publicationIds) {
            chapters.add(new SeriesItem(publicationId, chapters.size() + 1));
        }
        return new Series(id, title, slug, descriptionMarkdown, chapters, coverMediaId);
    }

    /**
     * Identifiants des articles, dans l'ordre des positions.
     */
    public List<Long> publicationIds() {
        return items.stream().map(SeriesItem::publicationId).toList();
    }

    /**
     * Articles de la série présents dans {@code publicationIds} (en pratique : les articles visibles), dans
     * l'ordre des positions. Leur rang dans cette liste, à partir de 1, est la position publique du chapitre
     * (D-BG) : un article masqué ne laisse pas de trou dans la numérotation.
     */
    public List<Long> publicationIdsAmong(Set<Long> publicationIds) {
        return items.stream()
            .map(SeriesItem::publicationId)
            .filter(publicationIds::contains)
            .toList();
    }

    /**
     * Navigation autour de l'article {@code publicationId} parmi les articles de la série présents dans
     * {@code publicationIds} (en pratique : les visibles), dans l'ordre des positions : un article masqué est
     * sauté (D-BM). Vide si l'article n'en fait pas partie.
     */
    public Optional<ChapterNavigation> navigationAround(Long publicationId, Set<Long> publicationIds) {
        List<Long> chapters = publicationIdsAmong(publicationIds);
        int index = chapters.indexOf(publicationId);
        if (index < 0) {
            return Optional.empty();
        }
        return Optional.of(new ChapterNavigation(
            index + 1,
            chapters.size(),
            index > 0 ? chapters.get(index - 1) : null,
            index + 1 < chapters.size() ? chapters.get(index + 1) : null));
    }
}
