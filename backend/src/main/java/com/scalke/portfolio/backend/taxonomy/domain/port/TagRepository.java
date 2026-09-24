package com.scalke.portfolio.backend.taxonomy.domain.port;

import com.scalke.portfolio.backend.taxonomy.domain.model.Tag;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Port de persistance des tags. Méthodes utilisées par {@code TaxonomyQueryService}
 * ({@code findBySlug}, {@code findAllById}) et par le seed de développement ({@code create}).
 */
public interface TagRepository {

    Optional<Tag> findBySlug(String slug);

    List<Tag> findAllById(Collection<Long> ids);

    Tag create(Tag tag);
}
