package com.scalke.portfolio.backend.publication.domain.port;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationFilter;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;

import java.time.Instant;
import java.util.Optional;

/**
 * Port de persistance des publications.
 * <p>
 * Ne contient que les méthodes utilisées par un appelant existant (ADR 0001) : {@code findVisible} par
 * {@code ListVisiblePublicationsUseCase}, {@code findVisibleBySlug} par {@code GetVisiblePublicationUseCase},
 * {@code existsAny} et {@code create} par le seed de développement.
 * <p>
 * « Visible à {@code now} » (D-AH) : {@code PUBLISHED}, ou {@code SCHEDULED} avec {@code publishedAt <= now}.
 * {@code now} est fourni par l'appelant, qui le lit dans l'horloge applicative.
 */
public interface PublicationRepository {

    /**
     * Publications visibles à {@code now}, restreintes par {@code filter}, les plus récentes d'abord
     * ({@code publishedAt} décroissant, puis identifiant décroissant : tri total).
     */
    PageResult<Publication> findVisible(PublicationFilter filter, Instant now, PageQuery query);

    /**
     * Vide si aucune publication ne porte ce slug ou si elle n'est pas visible à {@code now} :
     * les deux cas sont volontairement indiscernables.
     */
    Optional<Publication> findVisibleBySlug(String slug, Instant now);

    boolean existsAny();

    Publication create(Publication publication);
}
