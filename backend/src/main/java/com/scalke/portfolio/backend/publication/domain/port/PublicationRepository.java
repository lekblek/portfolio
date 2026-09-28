package com.scalke.portfolio.backend.publication.domain.port;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationFilter;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import com.scalke.portfolio.backend.shared.domain.model.Slug;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Port de persistance des publications.
 * <p>
 * Ne contient que les méthodes utilisées par un appelant existant (ADR 0001) : {@code findVisible} par
 * {@code ListVisiblePublicationsUseCase}, {@code findVisibleBySlug} par {@code GetVisiblePublicationUseCase},
 * {@code existsAny} et {@code create} par le seed de développement, {@code findById} et {@code updateStatus}
 * par {@code ChangePublicationStatusUseCase}, {@code findVisibleIds}, {@code findVisibleByIds} et
 * {@code findBySlug} par la façade {@code PublicationQueryService} (autres modules, ADR 0002).
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
    Optional<Publication> findVisibleBySlug(Slug slug, Instant now);

    /**
     * Identifiants, parmi {@code ids}, des publications visibles à {@code now} : une requête, sans charger
     * les publications.
     */
    Set<Long> findVisibleIds(Collection<Long> ids, Instant now);

    /**
     * Publications visibles à {@code now} parmi {@code ids}, dans un ordre quelconque.
     */
    List<Publication> findVisibleByIds(Collection<Long> ids, Instant now);

    /**
     * Toute publication portant ce slug, quel que soit son statut : jamais pour une lecture publique.
     */
    Optional<Publication> findBySlug(Slug slug);

    boolean existsAny();

    Publication create(Publication publication);

    /**
     * Toute publication, quel que soit son statut : réservé à l'écriture (administration), jamais aux
     * lectures publiques.
     */
    Optional<Publication> findById(Long id);

    /**
     * Enregistre un changement de statut (D-AX) : seuls {@code status}, {@code publishedAt} et
     * {@code updatedAt} sont écrits. La modification du contenu est une autre opération (étape 36).
     */
    Publication updateStatus(Publication publication);
}
