package com.scalke.portfolio.backend.project.domain.port;

import com.scalke.portfolio.backend.project.domain.model.Project;
import com.scalke.portfolio.backend.project.domain.model.ProjectFilter;
import com.scalke.portfolio.backend.shared.domain.model.PageQuery;
import com.scalke.portfolio.backend.shared.domain.model.PageResult;

import java.util.Optional;

/**
 * Port de persistance des projets.
 * <p>
 * Ne contient que les méthodes utilisées par un appelant existant (ADR 0001) :
 * {@code findPublished} par {@code ListPublishedProjectsUseCase}, {@code findPublishedBySlug} par
 * {@code GetPublishedProjectUseCase}, {@code existsAny} et {@code create} par le seed de développement.
 * La modification arrivera avec l'administration (étape 36).
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
    Optional<Project> findPublishedBySlug(String slug);

    boolean existsAny();

    /**
     * Crée un projet. Ses technologies doivent déjà exister (identifiant non nul) : un projet ne crée
     * jamais le vocabulaire (D-Z).
     */
    Project create(Project project);
}
