package com.scalke.portfolio.backend.taxonomy.domain.port;

import com.scalke.portfolio.backend.shared.domain.model.Slug;
import com.scalke.portfolio.backend.taxonomy.domain.model.Category;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Port de persistance des catégories. Méthodes utilisées par {@code TaxonomyQueryService} ({@code findBySlug},
 * {@code findAllById}), par le seed de développement ({@code create}) et par l'administration (D-CS) : les autres.
 */
public interface CategoryRepository {

    Optional<Category> findBySlug(Slug slug);

    List<Category> findAllById(Collection<Long> ids);

    /**
     * Toutes les catégories, par nom sans tenir compte de la casse.
     */
    List<Category> findAll();

    Optional<Category> findById(Long id);

    /**
     * Vrai si une autre catégorie que {@code excludedId} ({@code null} : aucune) porte ce slug.
     */
    boolean existsBySlug(Slug slug, Long excludedId);

    /**
     * Vrai si une autre catégorie que {@code excludedId} porte ce nom, sans tenir compte de la casse.
     */
    boolean existsByName(String name, Long excludedId);

    /**
     * @throws com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException nom ou slug pris entre-temps
     */
    Category create(Category category);

    /**
     * @throws com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException nom ou slug pris entre-temps
     */
    Category update(Category category);

    /**
     * @throws com.scalke.portfolio.backend.shared.error.BusinessRuleViolationException catégorie encore utilisée
     */
    void delete(Long id);
}
