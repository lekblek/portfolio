package com.scalke.portfolio.backend.project.domain.port;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectFilter;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;
import com.scalke.portfolio.backend.shared.domain.model.Slug;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Port de persistance des projets.
 * <p>
 * Ne contient que les méthodes utilisées par un appelant existant (ADR 0001) :
 * {@code findPublished} par {@code ListPublishedProjectsUseCase}, {@code findPublishedBySlug} par
 * {@code GetPublishedProjectUseCase}, {@code existsAny} et {@code create} par le seed de développement,
 * {@code searchPublished} et {@code findPublishedByIds} par la façade {@code ProjectQueryService} (autres
 * modules, ADR 0002) ; {@code findPage}, {@code findById}, {@code existsBySlug}, {@code create} et {@code update}
 * par l'administration (D-CX).
 */
public interface ProjectRepository {

    /**
     * Projets {@code PUBLISHED} uniquement, restreints par {@code filter}, dans l'ordre d'affichage public :
     * {@code displayOrder} croissant, puis date de début décroissante, puis identifiant (tri total).
     * Chaque projet est renvoyé avec ses technologies.
     */
    PageResult<Project> findPublished(ProjectFilter filter, PageQuery query);

    /**
     * Vide si aucun projet ne porte ce slug ou s'il n'est pas {@code PUBLISHED} : les deux cas sont
     * volontairement indiscernables (D-U).
     */
    Optional<Project> findPublishedBySlug(Slug slug);

    /**
     * Projets {@code PUBLISHED} parmi {@code ids}, avec leurs technologies, dans un ordre quelconque.
     */
    List<Project> findPublishedByIds(Collection<Long> ids);

    /**
     * Pertinence ({@code ts_rank}, D-CC) des projets {@code PUBLISHED} dont le document de recherche (titre,
     * technologies, description courte, description) correspond à {@code text}, lu comme une recherche web,
     * par identifiant. Vide si aucun ne correspond ou si le texte ne contient aucun mot recherchable.
     */
    Map<Long, Double> searchPublished(String text);

    boolean existsAny();

    /**
     * Tous les projets, quelle que soit leur visibilité (administration, D-CX), dans l'ordre d'affichage public.
     */
    PageResult<Project> findPage(PageQuery query);

    Optional<Project> findById(Long id);

    /**
     * Vrai si un autre projet que {@code excludedId} ({@code null} : aucun) porte ce slug.
     */
    boolean existsBySlug(Slug slug, Long excludedId);

    /**
     * Crée un projet, insertion exécutée immédiatement. Ses technologies doivent déjà exister (identifiant non nul) :
     * un projet ne crée jamais le vocabulaire (D-Z).
     *
     * @throws com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException slug pris entre-temps
     * @throws com.scalke.portfolio.backend.shared.error.InvalidInputException technologie ou média supprimé entre-temps
     */
    Project create(Project project);

    /**
     * Enregistre toute la saisie (technologies et captures remplacées) et la mémoire de publication. Écriture
     * exécutée immédiatement.
     *
     * @throws com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException slug pris entre-temps
     * @throws com.scalke.portfolio.backend.shared.error.InvalidInputException technologie ou média supprimé entre-temps
     */
    Project update(Project project);
}
