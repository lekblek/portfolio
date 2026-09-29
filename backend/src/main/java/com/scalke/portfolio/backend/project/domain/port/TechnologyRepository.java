package com.scalke.portfolio.backend.project.domain.port;

import com.scalke.portfolio.backend.project.domain.model.Technology;
import com.scalke.portfolio.backend.shared.domain.model.Slug;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Port de persistance du vocabulaire des technologies.
 * <p>
 * Ne contient que les méthodes utilisées par un appelant existant (ADR 0001) : {@code create} par le
 * seed de développement, les autres par l'administration du vocabulaire (D-CW) ; {@code findAllById} par celle des
 * projets (D-CX). La liste publique arrivera avec un écran qui en a besoin (D-AE).
 */
public interface TechnologyRepository {

    /**
     * Tout le vocabulaire, dans son ordre d'affichage ({@link Technology#DISPLAY_ORDER}).
     */
    List<Technology> findAll();

    Optional<Technology> findById(Long id);

    /**
     * Technologies parmi {@code ids} ; les inconnues sont absentes. Une requête.
     */
    List<Technology> findAllById(Collection<Long> ids);

    /**
     * Vrai si une autre technologie que {@code excludedId} ({@code null} : aucune) porte ce slug.
     */
    boolean existsBySlug(Slug slug, Long excludedId);

    /**
     * Vrai si une autre technologie que {@code excludedId} porte ce nom, sans tenir compte de la casse.
     */
    boolean existsByName(String name, Long excludedId);

    /**
     * @throws com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException nom ou slug pris entre-temps
     */
    Technology create(Technology technology);

    /**
     * @throws com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException nom ou slug pris entre-temps
     */
    Technology update(Technology technology);

    /**
     * @throws com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException technologie encore utilisée
     */
    void delete(Long id);
}
