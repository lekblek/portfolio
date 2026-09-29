package com.scalke.portfolio.backend.search.domain.model;

import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Contenu trouvé par la recherche, avant son chargement (D-CD) : module d'origine, identifiant dans ce module
 * et pertinence ({@code ts_rank}). Les deux modules calculent la pertinence avec la même configuration, les
 * mêmes poids et la même formule (D-CC) : leurs rangs sont comparables.
 */
public record SearchHit(Source source, long id, double rank) {

    public enum Source {
        PUBLICATION,
        PROJECT
    }

    /**
     * Ordre des résultats : pertinence décroissante ; à pertinence égale, les publications avant les projets,
     * puis le plus récemment créé (identifiant décroissant). Ordre total : la pagination est stable.
     */
    public static final Comparator<SearchHit> BY_RELEVANCE = Comparator
        .comparingDouble(SearchHit::rank).reversed()
        .thenComparing(SearchHit::source)
        .thenComparing(Comparator.comparingLong(SearchHit::id).reversed());

    public SearchHit {
        Objects.requireNonNull(source, "source");
        if (!(rank >= 0)) {
            throw new IllegalArgumentException("rank must be a non-negative number, got " + rank);
        }
    }

    /**
     * Page {@code query} des résultats classés : tous sont triés, seuls ceux de la page sont gardés, et
     * {@code totalElements} les compte tous.
     */
    public static PageResult<SearchHit> page(Collection<SearchHit> hits, PageQuery query) {
        List<SearchHit> content = hits.stream()
            .sorted(BY_RELEVANCE)
            .skip((long) query.page() * query.size())
            .limit(query.size())
            .toList();
        return new PageResult<>(content, query.page(), query.size(), hits.size());
    }
}
