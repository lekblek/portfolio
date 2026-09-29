package com.scalke.portfolio.backend.publication.domain.port;

import com.scalke.portfolio.backend.publication.domain.model.Publication;
import com.scalke.portfolio.backend.publication.domain.model.PublicationFilter;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import com.scalke.portfolio.backend.shared.domain.model.Slug;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Port de persistance des publications.
 * <p>
 * Ne contient que les méthodes utilisées par un appelant existant (ADR 0001) : {@code findVisible} par
 * {@code ListVisiblePublicationsUseCase}, {@code findVisibleBySlug} par {@code GetVisiblePublicationUseCase},
 * {@code existsAny} et {@code create} par le seed de développement, {@code findById} et {@code updateStatus}
 * par {@code ChangePublicationStatusUseCase}, {@code findVisibleIds}, {@code findVisibleByIds},
 * {@code findBySlug} et {@code searchVisible} par la façade {@code PublicationQueryService} (autres modules,
 * ADR 0002), qui utilise aussi {@code findVisibleBySlug} ; {@code findPage}, {@code existsBySlug}, {@code create},
 * {@code findById} et {@code update} par l'administration (D-CU).
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
     * Pertinence ({@code ts_rank}, D-CC) des publications visibles à {@code now} dont le document de recherche
     * (titre, tags, résumé, contenu) correspond à {@code text}, lu comme une recherche web, par identifiant.
     * Vide si aucune ne correspond ou si le texte ne contient aucun mot recherchable.
     */
    Map<Long, Double> searchVisible(String text, Instant now);

    /**
     * Toute publication portant ce slug, quel que soit son statut : jamais pour une lecture publique.
     */
    Optional<Publication> findBySlug(Slug slug);

    boolean existsAny();

    /**
     * Toutes les publications, quel que soit leur statut (administration, D-CU) : les dernières modifiées d'abord
     * ({@code updatedAt} décroissant, puis identifiant décroissant : tri total).
     */
    PageResult<Publication> findPage(PageQuery query);

    /**
     * Vrai si une autre publication que {@code excludedId} ({@code null} : aucune) porte ce slug.
     */
    boolean existsBySlug(Slug slug, Long excludedId);

    /**
     * Insertion exécutée immédiatement.
     *
     * @throws com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException slug pris entre-temps
     * @throws com.scalke.portfolio.backend.shared.error.InvalidInputException terme ou média supprimé entre-temps
     */
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

    /**
     * Enregistre la saisie de l'administrateur (D-CU) : titre, slug, résumé, contenu, mise en avant, termes,
     * couverture et SEO, avec {@code updatedAt}. Ni le type, ni le statut, ni ses dates. Écriture exécutée
     * immédiatement.
     *
     * @throws com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException slug pris entre-temps
     * @throws com.scalke.portfolio.backend.shared.error.InvalidInputException terme ou média supprimé entre-temps
     */
    Publication update(Publication publication);
}
