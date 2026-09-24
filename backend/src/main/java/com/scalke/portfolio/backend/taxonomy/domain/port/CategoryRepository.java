package com.scalke.portfolio.backend.taxonomy.domain.port;

import com.scalke.portfolio.backend.taxonomy.domain.model.Category;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Port de persistance des catégories. Méthodes utilisées par {@code TaxonomyQueryService}
 * ({@code findBySlug}, {@code findAllById}) et par le seed de développement ({@code create}).
 */
public interface CategoryRepository {

    Optional<Category> findBySlug(String slug);

    List<Category> findAllById(Collection<Long> ids);

    Category create(Category category);
}
