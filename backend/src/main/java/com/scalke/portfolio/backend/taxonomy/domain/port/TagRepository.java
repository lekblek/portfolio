package com.scalke.portfolio.backend.taxonomy.domain.port;

import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Port de persistance des tags. Méthodes utilisées par {@code TaxonomyQueryService} ({@code findBySlug},
 * {@code findAllById}), par le seed de développement ({@code create}) et par l'administration (D-CS) : les autres.
 */
public interface TagRepository {

    Optional<Tag> findBySlug(Slug slug);

    List<Tag> findAllById(Collection<Long> ids);

    /**
     * Tous les tags, par nom sans tenir compte de la casse.
     */
    List<Tag> findAll();

    Optional<Tag> findById(Long id);

    /**
     * Vrai si un autre tag que {@code excludedId} ({@code null} : aucun) porte ce slug.
     */
    boolean existsBySlug(Slug slug, Long excludedId);

    /**
     * Vrai si un autre tag que {@code excludedId} porte ce nom, sans tenir compte de la casse.
     */
    boolean existsByName(String name, Long excludedId);

    /**
     * @throws com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException nom ou slug pris entre-temps
     */
    Tag create(Tag tag);

    /**
     * @throws com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException nom ou slug pris entre-temps
     */
    Tag update(Tag tag);

    /**
     * @throws com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException tag encore utilisé
     */
    void delete(Long id);
}
