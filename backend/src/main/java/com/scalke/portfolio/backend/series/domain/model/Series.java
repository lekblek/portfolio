package com.scalke.portfolio.backend.series.domain.model;

import com.scalke.portfolio.backend.shared.domain.model.Slug;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Série d'articles (racine d'agrégat, D05) : liste ordonnée d'articles du module {@code publication},
 * référencés par identifiant (ADR 0002). Pas de workflow éditorial propre (01 §8) : une série est publique
 * si et seulement si au moins un de ses articles est visible (D-BG). Couverture : étape 27.
 * <p>
 * Invariants : une position au plus une fois (invariant 3), positions strictement positives, un article au
 * plus une fois. Doublés par PostgreSQL, qui garantit aussi qu'un article n'appartient qu'à une série et
 * qu'une {@code NEWS} n'y entre jamais (invariants 1 et 2, D-BF). Le slug est un {@link Slug} (D-BA) ; sa
 * stabilité après publication (D11) s'appliquera à sa modification (étape 36, D-BK).
 */
public record Series(
    Long id,
    String title,
    Slug slug,
    String descriptionMarkdown,
    List<SeriesItem> items
) {

    public Series {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(slug, "slug");
        Objects.requireNonNull(descriptionMarkdown, "descriptionMarkdown");
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
}
