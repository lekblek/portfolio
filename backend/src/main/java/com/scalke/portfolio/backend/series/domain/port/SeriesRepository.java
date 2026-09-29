package com.scalke.portfolio.backend.series.domain.port;

import com.scalke.portfolio.backend.series.domain.model.Series;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import com.scalke.portfolio.backend.shared.domain.model.Slug;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Port de persistance des séries.
 * <p>
 * Ne contient que les méthodes utilisées par un appelant existant (ADR 0001) : {@code findAllPublicationIds}
 * et {@code findHavingAnyPublication} par {@code ListVisibleSeriesUseCase}, {@code findBySlug} par
 * {@code GetVisibleSeriesUseCase}, {@code findByPublicationId} par {@code GetSeriesNavigationUseCase},
 * {@code existsAny} et {@code create} par le seed de développement ; {@code findPage}, {@code findById},
 * {@code existsBySlug}, {@code findPublicationIdsInOtherSeries}, {@code create} et {@code update} par
 * l'administration (D-CV).
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

    /**
     * La série contenant cet article ; au plus une (invariant 2, D05).
     */
    Optional<Series> findByPublicationId(Long publicationId);

    boolean existsAny();

    /**
     * Toutes les séries (administration, D-CV), par titre sans tenir compte de la casse puis par identifiant.
     */
    PageResult<Series> findPage(PageQuery query);

    Optional<Series> findById(Long id);

    /**
     * Vrai si une autre série que {@code excludedId} ({@code null} : aucune) porte ce slug.
     */
    boolean existsBySlug(Slug slug, Long excludedId);

    /**
     * Articles, parmi {@code publicationIds}, déjà rangés dans une autre série que {@code seriesId}.
     */
    Set<Long> findPublicationIdsInOtherSeries(Collection<Long> publicationIds, Long seriesId);

    /**
     * Insertion exécutée immédiatement.
     *
     * @throws com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException slug pris entre-temps
     */
    Series create(Series series);

    /**
     * Enregistre titre, slug, description, couverture et chapitres (remplacés d'un bloc). Écriture exécutée
     * immédiatement.
     *
     * @throws com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException slug pris ou article rangé
     *         dans une autre série entre-temps
     */
    Series update(Series series);
}
