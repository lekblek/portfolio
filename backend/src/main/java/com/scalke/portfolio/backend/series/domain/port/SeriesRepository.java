package com.scalke.portfolio.backend.series.domain.port;

import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import com.scalke.portfolio.backend.shared.domain.model.Slug;

import java.util.Optional;
import java.util.Set;

/**
 * Port de persistance des séries.
 * <p>
 * Ne contient que les méthodes utilisées par un appelant existant (ADR 0001) : {@code findAllPublicationIds}
 * et {@code findHavingAnyPublication} par {@code ListVisibleSeriesUseCase}, {@code findBySlug} par
 * {@code GetVisibleSeriesUseCase}, {@code existsAny} et {@code create} par le seed de développement.
 * <p>
 * Le port ne connaît pas la visibilité des articles, qui appartient au module {@code publication} : les
 * cas d'usage la lui demandent par sa façade et transmettent des identifiants (D-BG, D-BJ).
 */
public interface SeriesRepository {

    /**
     * Identifiants de tous les articles rangés dans une série.
     */
    Set<Long> findAllPublicationIds();

    /**
     * Séries contenant au moins un des articles donnés, par titre (sans tenir compte de la casse) puis par
     * identifiant : tri total.
     */
    PageResult<Series> findHavingAnyPublication(Set<Long> publicationIds, PageQuery query);

    /**
     * Toute série portant ce slug, visible ou non : la visibilité est décidée par l'appelant.
     */
    Optional<Series> findBySlug(Slug slug);

    boolean existsAny();

    Series create(Series series);
}
